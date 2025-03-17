import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

public class Main extends JFrame {
    private JTextArea inputArea;         // For entering code.
    public static JTextArea consoleArea; // Console for both output and input.
    private JButton runButton, clearButton, stopButton;

    // For interactive input handling.
    public static final Object inputLock = new Object();
    public static int inputStart = 0; // Position in the console document where user input begins.

    // Reference to the currently running interpreter thread.
    private Thread currentThread = null;

    public Main() {
        setTitle("Code Interpreter - Night Mode");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Color backgroundDark = new Color(30, 30, 30);
        Color foregroundLight = new Color(220, 220, 220);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundDark);
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        // Button Panel at the top.
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(backgroundDark);
        runButton = new JButton("Run");
        clearButton = new JButton("Clear");
        stopButton = new JButton("Stop");
        styleButton(runButton);
        styleButton(clearButton);
        styleButton(stopButton);
        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(stopButton);

        // Input Panel for code entry.
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(backgroundDark);
        inputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(foregroundLight),
                "Enter Code",
                0, 0, null, foregroundLight));
        inputArea = new JTextArea(10, 50);
        inputArea.setFont(new Font("Consolas", Font.BOLD, 18));
        inputArea.setMargin(new Insets(10, 10, 10, 10));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setTabSize(4);
        inputArea.setBackground(backgroundDark);
        inputArea.setForeground(foregroundLight);
        inputArea.setCaretColor(Color.WHITE);

        // Disable focus traversal on Tab and add a key listener that inserts 4 spaces.
        inputArea.setFocusTraversalKeysEnabled(false);
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    int pos = inputArea.getCaretPosition();
                    // Insert four spaces instead of a tab character.
                    inputArea.insert("    ", pos);
                    e.consume();
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Console Panel for output and interactive input.
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(backgroundDark);
        consolePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(foregroundLight),
                "Console",
                0, 0, null, foregroundLight));
        consoleArea = new JTextArea(15, 50);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        consoleArea.setMargin(new Insets(10, 10, 10, 10));
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setBackground(backgroundDark);
        consoleArea.setForeground(foregroundLight);
        consoleArea.setCaretColor(Color.WHITE);
        // Make the console area uneditable by default.
        consoleArea.setEditable(false);
        // Install a DocumentFilter to restrict editing to positions after inputStart.
        ((AbstractDocument) consoleArea.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void remove(FilterBypass fb, int offset, int length)
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.remove(fb, offset, length);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (offset >= inputStart) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });
        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consolePanel.add(consoleScroll, BorderLayout.CENTER);

        // Add a CaretListener to force the caret to stay at or after inputStart.
        consoleArea.addCaretListener(e -> {
            if (consoleArea.getCaretPosition() < inputStart) {
                SwingUtilities.invokeLater(() -> consoleArea.setCaretPosition(inputStart));
            }
        });

        // Add a MouseListener so that clicking before inputStart moves the caret.
        consoleArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (consoleArea.getCaretPosition() < inputStart) {
                    consoleArea.setCaretPosition(inputStart);
                }
            }
        });

        // Add a KeyListener to intercept Enter key presses.
        consoleArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Only handle Enter if the caret is in the editable (input prompt) region.
                    if (consoleArea.getCaretPosition() >= inputStart) {
                        e.consume(); // Prevent insertion of newline.
                        SwingUtilities.invokeLater(() -> {
                            consoleArea.append("\n");
                            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                        });
                        synchronized (inputLock) {
                            inputLock.notify();
                        }
                    }
                }
            }
        });

        // Create a JSplitPane for code input and console.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, consolePanel);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(20);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(foregroundLight);
                        int thickness = 2;
                        if (getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                            int y = getHeight() / 2 - thickness / 2;
                            g.fillRect(0, y, getWidth(), thickness);
                        } else {
                            int x = getWidth() / 2 - thickness / 2;
                            g.fillRect(x, 0, thickness, getHeight());
                        }
                    }
                };
            }
        });

        contentPane.add(buttonPanel, BorderLayout.NORTH);
        contentPane.add(splitPane, BorderLayout.CENTER);

        // Button Actions.
        runButton.addActionListener((ActionEvent e) -> runCode());
        clearButton.addActionListener((ActionEvent e) -> {
            inputArea.setText("");
            consoleArea.setText("");
        });
        stopButton.addActionListener((ActionEvent e) -> stopExecution());

        setVisible(true);
    }

    // Style a button.
    private void styleButton(JButton button) {
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Consolas", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(80, 40));
    }

    // Append text to the console area.
    public static void appendToConsole(String text) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(text);
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    // Run the code in a background thread ensuring a fresh execution.
    private void runCode() {
        String code = inputArea.getText().trim();
        if (code.isEmpty()) {
            consoleArea.setText("Error: No code entered.\n");
            return;
        }

        // Ensure any previous execution is stopped and environment reset.
        stopExecution();
        consoleArea.setText(""); // Clear the console output.
        inputStart = 0;          // Reset the input marker.

        currentThread = new Thread(() -> {
            try {
                // Redirect output to console.
                PrintStream ps = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        SwingUtilities.invokeLater(() -> {
                            consoleArea.append(String.valueOf((char) b));
                            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                        });
                    }
                });
                PrintStream originalOut = System.out;
                System.setOut(ps);

                // Tokenization.
                Lexer lexer = new Lexer(code);
                List<Token> tokens = lexer.tokenize();
                System.out.println("Tokens:");
                for (Token token : tokens) {
                    System.out.println(token);
                }

                // Parsing.
                System.out.println("\nParsing process:");
                Parser parser = new Parser(tokens);
                ASTNode ast = parser.parse();
                System.out.println("\nFinal Parsed AST:");
                System.out.println(printAST(ast, 0));

                // Create a fresh interpreter instance for this run.
                Interpreter interpreter = new Interpreter();

                // Interpretation.
                System.out.println("\nInterpreting...\nResult: ");
                interpreter.evaluate(ast);

                System.out.flush();
                System.setOut(originalOut);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> consoleArea.setText("Error: " + ex.getMessage()));
            }
        });
        currentThread.start();
    }

    // Stop button action: interrupt running thread and clear output.
    private void stopExecution() {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
            currentThread = null;
        }
        consoleArea.setText("");
        inputStart = 0;          
    }

    // Helper method to print the AST (if needed for debugging).
    private String printAST(ASTNode node, int level) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        String indent = "  ".repeat(level);
        if (node instanceof NumberNode) {
            sb.append(indent).append("Number: ").append(((NumberNode) node).token.value).append("\n");
        } else if (node instanceof StringNode) {
            sb.append(indent).append("String: \"").append(((StringNode) node).token.value).append("\"\n");
        } else if (node instanceof IdentifierNode) {
            sb.append(indent).append("Identifier: ").append(((IdentifierNode) node).identifier.value).append("\n");
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node;
            sb.append(indent).append("BinaryOp: ").append(binOp.op.value).append("\n");
            sb.append(printAST(binOp.left, level + 1));
            sb.append(printAST(binOp.right, level + 1));
        } else if (node instanceof UnaryOpNode) {
            UnaryOpNode unary = (UnaryOpNode) node;
            sb.append(indent).append("UnaryOp: ").append(unary.op.value).append("\n");
            sb.append(printAST(unary.operand, level + 1));
        } else if (node instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) node;
            sb.append(indent).append("Assignment:\n");
            sb.append(indent).append("  Identifier: ").append(assign.identifier.value).append("\n");
            sb.append(indent).append("  Value:\n");
            sb.append(printAST(assign.value, level + 2));
        } else if (node instanceof PrintNode) {
            PrintNode printNode = (PrintNode) node;
            sb.append(indent).append("Print:\n");
            sb.append(printAST(printNode.expression, level + 1));
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            sb.append(indent).append("Block:\n");
            for (ASTNode stmt : block.statements) {
                sb.append(printAST(stmt, level + 1));
            }
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            sb.append(indent).append("If Statement:\n");
            sb.append(indent).append("  Condition:\n").append(printAST(ifNode.condition, level + 2));
            sb.append(indent).append("  Then Branch:\n").append(printAST(ifNode.thenBranch, level + 2));
            if (ifNode.elseBranch != null) {
                sb.append(indent).append("  Else Branch:\n").append(printAST(ifNode.elseBranch, level + 2));
            }
        } else if (node instanceof FunctionDefinitionNode) {
            FunctionDefinitionNode funcDef = (FunctionDefinitionNode) node;
            sb.append(indent).append("FunctionDefinition: ").append(funcDef.name.value).append("\n");
            sb.append(indent).append("  Parameters: ");
            if (funcDef.parameters.isEmpty()) {
                sb.append("None\n");
            } else {
                for (Token param : funcDef.parameters) {
                    sb.append(param.value).append(" ");
                }
                sb.append("\n");
            }
            sb.append(indent).append("  Body:\n");
            sb.append(printAST(funcDef.body, level + 2));
        } else if (node instanceof FunctionCallNode) {
            FunctionCallNode funcCall = (FunctionCallNode) node;
            sb.append(indent).append("FunctionCall: ").append(funcCall.name.value).append("\n");
            sb.append(indent).append("  Arguments:\n");
            for (ASTNode arg : funcCall.arguments) {
                sb.append(printAST(arg, level + 2));
            }
        } else if (node instanceof ClassDefinitionNode) {
            ClassDefinitionNode classDef = (ClassDefinitionNode) node;
            sb.append(indent).append("ClassDefinition: ").append(classDef.name.value).append("\n");
            sb.append(indent).append("  Members:\n");
            for (ASTNode member : classDef.members) {
                sb.append(printAST(member, level + 2));
            }
        } else if (node instanceof ObjectCreationNode) {
            ObjectCreationNode objCreation = (ObjectCreationNode) node;
            sb.append(indent).append("ObjectCreation: ").append(objCreation.className.value).append("\n");
            sb.append(indent).append("  Arguments:\n");
            for (ASTNode arg : objCreation.arguments) {
                sb.append(printAST(arg, level + 2));
            }
        } else if (node instanceof MethodCallNode) {
            MethodCallNode methodCall = (MethodCallNode) node;
            sb.append(indent).append("MethodCall: ").append(methodCall.methodName.value).append("\n");
            sb.append(indent).append("  Target:\n").append(printAST(methodCall.target, level + 2));
            sb.append(indent).append("  Arguments:\n");
            for (ASTNode arg : methodCall.arguments) {
                sb.append(printAST(arg, level + 2));
            }
        } else {
            sb.append(indent).append("Unknown Node: ").append(node.getClass().getSimpleName()).append("\n");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}
