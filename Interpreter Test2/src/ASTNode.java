import java.util.List;

abstract class ASTNode {
    // Field to track the line number.
    public int line = -1;
}

class NumberNode extends ASTNode {
    Token token;
    public NumberNode(Token token) {
        this.token = token;
        this.line = token.line;
    }
}

class BinaryOpNode extends ASTNode {
    Token op;
    ASTNode left;
    ASTNode right;
    public BinaryOpNode(ASTNode left, Token token, ASTNode right) {
        this.op = token;
        this.left = left;
        this.right = right;
        this.line = token.line;
    }
}

class UnaryOpNode extends ASTNode {
    Token op;
    ASTNode operand;
    public UnaryOpNode(Token op, ASTNode operand) {
        this.op = op;
        this.operand = operand;
        this.line = op.line;
    }
}

class AssignmentNode extends ASTNode {
    Token identifier;
    ASTNode value;
    public AssignmentNode(ASTNode value, Token identifier) {
        this.value = value;
        this.identifier = identifier;
        this.line = identifier.line;
    }
}

class IdentifierNode extends ASTNode {
    Token identifier;
    public IdentifierNode(Token identifier) {
        this.identifier = identifier;
        this.line = identifier.line;
    }
}

class BlockNode extends ASTNode {
    List<ASTNode> statements;
    public BlockNode(List<ASTNode> statements, int line) {
        this.statements = statements;
        this.line = line;
    }
    public BlockNode(List<ASTNode> statements) {
        this(statements, -1);
    }
}

class FunctionDefinitionNode extends ASTNode {
    Token name;
    List<Token> parameters;
    ASTNode body;
    public FunctionDefinitionNode(Token name, List<Token> parameters, ASTNode body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
        this.line = name.line;
    }
}

class FunctionCallNode extends ASTNode {
    Token name;
    List<ASTNode> arguments;
    public FunctionCallNode(Token name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
        this.line = name.line;
    }
}

// For string literals.
class StringNode extends ASTNode {
    Token token;
    public StringNode(Token token) {
        this.token = token;
        this.line = token.line;
    }
}

// For if-statements with optional else-branches.
class IfNode extends ASTNode {
    ASTNode condition;
    ASTNode thenBranch;
    ASTNode elseBranch; // May be null if no else clause exists.
    public IfNode(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
        if (condition != null) {
            this.line = condition.line;
        }
    }
}

class PrintNode extends ASTNode {
    public ASTNode expression;
    public PrintNode(ASTNode expression) {
        this.expression = expression;
        this.line = expression.line;
    }
}

class ReturnNode extends ASTNode {
    public ASTNode expression;
    public ReturnNode(ASTNode expression) {
        this.expression = expression;
        this.line = expression.line;
    }
}

class ClassDefinitionNode extends ASTNode {
    public Token name;
    public List<ASTNode> members;
    public ClassDefinitionNode(Token name, List<ASTNode> members) {
        this.name = name;
        this.members = members;
        this.line = name.line;
    }
}

class ObjectCreationNode extends ASTNode {
    public Token className;
    public List<ASTNode> arguments;
    public ObjectCreationNode(Token className, List<ASTNode> arguments) {
        this.className = className;
        this.arguments = arguments;
        this.line = className.line;
    }
}

class MethodCallNode extends ASTNode {
    public ASTNode target;         // The object on which the method is called.
    public Token methodName;       // The token representing the method name.
    public List<ASTNode> arguments; // The list of arguments passed to the method.
    public MethodCallNode(ASTNode target, Token methodName, List<ASTNode> arguments) {
        this.target = target;
        this.methodName = methodName;
        this.arguments = arguments;
        this.line = methodName.line;
    }

    @Override
    public String toString() {
        return "MethodCall(" + target + "." + methodName.value + ", args=" + arguments + ")";
    }
}

class FieldAccessNode extends ASTNode {
    public ASTNode target;
    public Token fieldName;
    public FieldAccessNode(ASTNode target, Token fieldName) {
        this.target = target;
        this.fieldName = fieldName;
        this.line = fieldName.line;
    }
}

class FieldAssignmentNode extends ASTNode {
    public ASTNode target;
    public Token fieldName;
    public ASTNode value;
    public FieldAssignmentNode(ASTNode target, Token fieldName, ASTNode value) {
        this.target = target;
        this.fieldName = fieldName;
        this.value = value;
        this.line = fieldName.line;
    }
}

class BooleanNode extends ASTNode {
    public Token token;
    public BooleanNode(Token token) {
        this.token = token;
        this.line = token.line;
    }
}

class WhileNode extends ASTNode {
    public ASTNode condition;
    public ASTNode body;
    public WhileNode(ASTNode condition, ASTNode body) {
        this.condition = condition;
        this.body = body;
        if (condition != null) {
            this.line = condition.line;
        }
    }
}

class ForNode extends ASTNode {
    public Token loopVar;
    public ASTNode start;
    public ASTNode end;
    public ASTNode body;
    public ForNode(Token loopVar, ASTNode start, ASTNode end, ASTNode body) {
        this.loopVar = loopVar;
        this.start = start;
        this.end = end;
        this.body = body;
        this.line = loopVar.line;
    }
}

class ListNode extends ASTNode {
    private final List<ASTNode> elements;
    public ListNode(List<ASTNode> elements) {
        this.elements = elements;
    }
    public List<ASTNode> getElements() {
        return elements;
    }
    @Override
    public String toString() {
        return "ListNode{" + elements + "}";
    }
}

class IndexNode extends ASTNode {
    private ASTNode base;
    private ASTNode index;
    public IndexNode(ASTNode base, ASTNode index) {
        this.base = base;
        this.index = index;
        if (base != null) {
            this.line = base.line;
        }
    }
    public ASTNode getBase() {
        return base;
    }
    public ASTNode getIndex() {
        return index;
    }
    @Override
    public String toString() {
        return "IndexNode(" + base + ", " + index + ")";
    }
}

class IndexAssignmentNode extends ASTNode {
    private ASTNode target;
    private ASTNode value;
    public IndexAssignmentNode(ASTNode target, ASTNode value) {
        this.target = target;
        this.value = value;
        if (target != null) {
            this.line = target.line;
        }
    }
    public ASTNode getTarget() {
        return target;
    }
    public ASTNode getValue() {
        return value;
    }
    @Override
    public String toString() {
        return "IndexAssignmentNode(" + target + " = " + value + ")";
    }
}

class SliceNode extends ASTNode {
    private ASTNode target;
    private ASTNode start;
    private ASTNode end;
    private ASTNode step;
    public SliceNode(ASTNode target, ASTNode start, ASTNode end, ASTNode step) {
        this.target = target;
        this.start = start;
        this.end = end;
        this.step = step;
        if (target != null) {
            this.line = target.line;
        }
    }
    public ASTNode getTarget() { return target; }
    public ASTNode getStart() { return start; }
    public ASTNode getEnd() { return end; }
    public ASTNode getStep() { return step; }
    @Override
    public String toString() {
        return "SliceNode(" + target + ", " + start + ", " + end + ", " + step + ")";
    }
}

class ForEachNode extends ASTNode {
    private Token loopVar;
    private ASTNode listExpr;
    private ASTNode body;
    public ForEachNode(Token loopVar, ASTNode listExpr, ASTNode body) {
        this.loopVar = loopVar;
        this.listExpr = listExpr;
        this.body = body;
        this.line = loopVar.line;
    }
    public Token getLoopVar() {
        return loopVar;
    }
    public ASTNode getListExpr() {
        return listExpr;
    }
    public ASTNode getBody() {
        return body;
    }
}
