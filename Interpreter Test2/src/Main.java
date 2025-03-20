import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.*;
import javax.swing.event.*;
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
    private final JTextArea inputArea;
    public static JTextArea consoleArea;
    private final JButton runButton, clearButton, stopButton;

    // For interactive input handling.
    public static final Object inputLock = new Object();
    public static int inputStart = 0; // Position in the console document where user input begins.

    // Reference to the currently running interpreter thread.
    private Thread currentThread = null;

    public Main() {
        setTitle("Code Interpreter - Dark Theme");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Modern dark theme colors
        Color backgroundDark = new Color(1, 22, 39);      // Deep blue-black
        Color panelDark = new Color(5, 29, 48);          // Slightly lighter blue
        Color foregroundLight = new Color(226, 232, 240); // Off-white text
        Color borderColor = new Color(51, 65, 85);       // Border color
        Color highlightColor = new Color(96, 165, 250);  // Highlight color
        Color lineNumberColor = new Color(75, 85, 99);   // Line number color

        // Button colors
        Color runButtonColor = new Color(34, 197, 94);    // Green
        Color runButtonHover = new Color(22, 163, 74);    // Darker green
        Color clearButtonColor = new Color(234, 179, 8);  // Yellow
        Color clearButtonHover = new Color(202, 138, 4);  // Darker yellow
        Color stopButtonColor = new Color(239, 68, 68);   // Red
        Color stopButtonHover = new Color(220, 38, 38);   // Darker red

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundDark);
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(contentPane);

        // Button Panel with colorful buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        buttonPanel.setBackground(backgroundDark);
        
        runButton = new JButton("Run");
        clearButton = new JButton("Clear");
        stopButton = new JButton("Stop");
        
        // Style each button with its own color
        styleButton(runButton, runButtonColor, runButtonHover);
        styleButton(clearButton, clearButtonColor, clearButtonHover);
        styleButton(stopButton, stopButtonColor, stopButtonHover);
        
        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(stopButton);

        // Input Panel with IDE-style code editor
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(panelDark);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel inputLabel = new JLabel("Code Editor");
        inputLabel.setForeground(highlightColor);
        inputLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 0));
        inputPanel.add(inputLabel, BorderLayout.NORTH);

        inputArea = new JTextArea(10, 50);
        inputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        inputArea.setMargin(new Insets(15, 15, 15, 15));
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setTabSize(2);
        inputArea.setBackground(panelDark);
        inputArea.setForeground(foregroundLight);
        inputArea.setCaretColor(highlightColor);
        inputArea.setSelectionColor(new Color(53, 72, 94));
        inputArea.setSelectedTextColor(foregroundLight);

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

        // Create line numbers for input
        JTextArea inputLineNumbers = new JTextArea("1");
        inputLineNumbers.setBackground(panelDark);
        inputLineNumbers.setForeground(lineNumberColor);
        inputLineNumbers.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        inputLineNumbers.setEditable(false);
        inputLineNumbers.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));
        
        // Update line numbers when text changes
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void insertUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void removeUpdate(DocumentEvent e) { updateLineNumbers(); }
            
            private void updateLineNumbers() {
                String text = inputArea.getText();
                int lines = text.split("\n").length;
                StringBuilder numbers = new StringBuilder();
                for (int i = 1; i <= lines; i++) {
                    numbers.append(i).append("\n");
                }
                inputLineNumbers.setText(numbers.toString());
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        inputScroll.getViewport().setBackground(panelDark);
        inputScroll.setRowHeaderView(inputLineNumbers);
        styleScrollPane(inputScroll, panelDark, highlightColor);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Console Panel with IDE-style output
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(panelDark);
        consolePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel consoleLabel = new JLabel("Console Output");
        consoleLabel.setForeground(highlightColor);
        consoleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 0));
        consolePanel.add(consoleLabel, BorderLayout.NORTH);

        consoleArea = new JTextArea(15, 50);
        consoleArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        consoleArea.setMargin(new Insets(15, 15, 15, 15));
        consoleArea.setLineWrap(true);
        consoleArea.setWrapStyleWord(true);
        consoleArea.setBackground(panelDark);
        consoleArea.setForeground(foregroundLight);
        consoleArea.setCaretColor(highlightColor);
        consoleArea.setSelectionColor(new Color(53, 72, 94));
        consoleArea.setSelectedTextColor(foregroundLight);

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

        // Create line numbers for console
        JTextArea consoleLineNumbers = new JTextArea("1");
        consoleLineNumbers.setBackground(panelDark);
        consoleLineNumbers.setForeground(lineNumberColor);
        consoleLineNumbers.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        consoleLineNumbers.setEditable(false);
        consoleLineNumbers.setBorder(BorderFactory.createEmptyBorder(15, 5, 15, 5));
        
        // Update console line numbers when text changes
        consoleArea.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void insertUpdate(DocumentEvent e) { updateLineNumbers(); }
            public void removeUpdate(DocumentEvent e) { updateLineNumbers(); }
            
            private void updateLineNumbers() {
                String text = consoleArea.getText();
                int lines = text.split("\n").length;
                StringBuilder numbers = new StringBuilder();
                for (int i = 1; i <= lines; i++) {
                    numbers.append(i).append("\n");
                }
                consoleLineNumbers.setText(numbers.toString());
            }
        });

        JScrollPane consoleScroll = new JScrollPane(consoleArea);
        consoleScroll.setBorder(null);
        consoleScroll.getViewport().setBackground(panelDark);
        consoleScroll.setRowHeaderView(consoleLineNumbers);
        styleScrollPane(consoleScroll, panelDark, highlightColor);
        consolePanel.add(consoleScroll, BorderLayout.CENTER);

        // Create a JSplitPane with IDE-style divider
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, consolePanel);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(6);
        splitPane.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(borderColor);
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(highlightColor);
                        if (getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                            g.fillRect(0, getHeight()/2 - 1, getWidth(), 2);
                        } else {
                            g.fillRect(getWidth()/2 - 1, 0, 2, getHeight());
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

    // Enhanced button styling
    private void styleButton(JButton button, Color buttonColor, Color hoverColor) {
        button.setBackground(buttonColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(90, 32));
        
        // Add hover effect with shadow
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 255, 255, 50), 1),
                    BorderFactory.createEmptyBorder(7, 15, 7, 15)
                ));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(buttonColor);
                button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(hoverColor.darker());
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(hoverColor);
            }
        });
    }

    // Style scroll panes
    private void styleScrollPane(JScrollPane scrollPane, Color backgroundColor, Color highlightColor) {
        scrollPane.setBackground(backgroundColor);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = highlightColor;
                this.trackColor = backgroundColor;
                this.thumbDarkShadowColor = highlightColor;
                this.thumbLightShadowColor = highlightColor;
                this.thumbHighlightColor = highlightColor;
            }
        });
        scrollPane.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = highlightColor;
                this.trackColor = backgroundColor;
                this.thumbDarkShadowColor = highlightColor;
                this.thumbLightShadowColor = highlightColor;
                this.thumbHighlightColor = highlightColor;
            }
        });
    }

    // Run the code in a background thread ensuring a fresh execution.
    private void runCode() {
        String code = inputArea.getText();
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
//                System.out.println("Tokens:");
//                for (Token token : tokens) {
//                    System.out.println(token);
//                }

                // Parsing.
//                System.out.println("\nParsing process:");
                Parser parser = new Parser(tokens);
                ASTNode ast = parser.parse();
//                System.out.println("\nFinal Parsed AST:");
//                System.out.println(printAST(ast, 0));

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
