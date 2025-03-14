import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

public class Main extends JFrame {
    private JTextArea inputArea;
    private JTextArea outputArea;
    private JButton runButton, clearButton;

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

        // Button Panel at top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(backgroundDark);
        runButton = new JButton("Run");
        clearButton = new JButton("Clear");

        // Style the buttons with a fixed size and white fill
        styleButton(runButton);
        styleButton(clearButton);
        buttonPanel.add(runButton);
        buttonPanel.add(clearButton);

        // Input Panel
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
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // Output Panel
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBackground(backgroundDark);
        outputPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(foregroundLight),
                "Output",
                0, 0, null, foregroundLight));
        outputArea = new JTextArea(15, 50);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        outputArea.setMargin(new Insets(10, 10, 10, 10));
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBackground(backgroundDark);
        outputArea.setForeground(foregroundLight);
        outputArea.setCaretColor(Color.WHITE);
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        // Create a JSplitPane with a thicker grab area but visually a thin divider.
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputPanel, outputPanel);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(20); // Thicker grab area

        // Customize divider appearance: draw a 2-pixel line in the center.
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

        // Button Actions
        runButton.addActionListener((ActionEvent e) -> runCode());
        clearButton.addActionListener((ActionEvent e) -> {
            inputArea.setText("");
            outputArea.setText("");
        });

        setVisible(true);
    }

    private void styleButton(JButton button) {
        // Fill the button with white and set text to black.
        button.setBackground(Color.WHITE);
        button.setForeground(Color.BLACK);
        // Use a larger, bold font.
        button.setFont(new Font("Consolas", Font.BOLD, 16));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Set a fixed size to ensure both buttons are uniform and bigger.
        button.setPreferredSize(new Dimension(80, 40));
    }

    private void runCode() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("Error: No code entered.");
            return;
        }
        try {
            ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputCapture));

            // Tokenization
            Lexer lexer = new Lexer(input);
            List<Token> tokens = lexer.tokenize();
            StringBuilder output = new StringBuilder("Tokens:\n");
            for (Token token : tokens) {
                output.append(token).append("\n");
            }

            // Parsing
            output.append("\nParsing process:\n");
            Parser parser = new Parser(tokens);
            ASTNode ast = parser.parse();
            output.append("\nFinal Parsed AST:\n");
            output.append(printAST(ast, 0));

            // Interpretation
            output.append("\nInterpreting...\n");
            Interpreter interpreter = new Interpreter();
            Object result = interpreter.evaluate(ast);
            System.out.flush();
            System.setOut(originalOut);

            if (result != null) {
                output.append("\nResult:\n").append(result);
            } else {
                output.append("\nResult: No Output");
            }
            String capturedOutput = outputCapture.toString();
            if (!capturedOutput.isEmpty()) {
                output.append("\n").append(capturedOutput);
            }
            outputArea.setText(output.toString());
        } catch (Exception ex) {
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

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
