import java.util.List;

abstract class ASTNode {}

class NumberNode extends ASTNode {
    Token token;
    public NumberNode(Token token) {
        this.token = token;
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
    }
}
class UnaryOpNode extends ASTNode {
    Token op;
    ASTNode operand;
    public UnaryOpNode(Token op, ASTNode operand) {
        this.op = op;
        this.operand = operand;
    }
}
class AssignmentNode extends ASTNode {
    Token identifier;
    ASTNode value;
    public AssignmentNode(ASTNode value, Token identifier) {
        this.value = value;
        this.identifier = identifier;
    }
}

class IdentifierNode extends ASTNode {
    Token identifier;
    public IdentifierNode(Token identifier) {
        this.identifier = identifier;
    }
}
class BlockNode extends ASTNode {
    List<ASTNode> statements;
    public BlockNode(List<ASTNode> statements) {
        this.statements = statements;
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
    }
}
class FunctionCallNode extends ASTNode {
    Token name;
    List<ASTNode> arguments;
    public FunctionCallNode(Token name, List<ASTNode> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}
// For string literals.
class StringNode extends ASTNode {
    Token token;
    public StringNode(Token token) {
        this.token = token;
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
    }
}
class PrintNode extends ASTNode {
    public ASTNode expression;

    public PrintNode(ASTNode expression) {
        this.expression = expression;
    }
}
class ReturnNode extends ASTNode {
    public ASTNode expression;

    public ReturnNode(ASTNode expression) {
        this.expression = expression;
    }
}
class ClassDefinitionNode extends ASTNode {
    public Token name;
    public List<ASTNode> members;

    public ClassDefinitionNode(Token name, List<ASTNode> members) {
        this.name = name;
        this.members = members;
    }
}

class ObjectCreationNode extends ASTNode {
    public Token className;
    public List<ASTNode> arguments;

    public ObjectCreationNode(Token className, List<ASTNode> arguments) {
        this.className = className;
        this.arguments = arguments;
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
    }
}

class FieldAssignmentNode extends ASTNode {
    public ASTNode target;   // e.g., the "self" expression
    public Token fieldName;  // e.g., the token for "name"
    public ASTNode value;    // The right-hand side expression

    public FieldAssignmentNode(ASTNode target, Token fieldName, ASTNode value) {
        this.target = target;
        this.fieldName = fieldName;
        this.value = value;
    }
}
class BooleanNode extends ASTNode {
    public Token token;

    public BooleanNode(Token token) {
        this.token = token;
    }
}
class WhileNode extends ASTNode {
    public ASTNode condition;
    public ASTNode body;
    public WhileNode(ASTNode condition, ASTNode body) {
        this.condition = condition;
        this.body = body;
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
    private ASTNode target; // Typically an IndexNode or FieldAccessNode.
    private ASTNode value;

    public IndexAssignmentNode(ASTNode target, ASTNode value) {
        this.target = target;
        this.value = value;
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
    private ASTNode start; // may be null
    private ASTNode end;   // may be null
    private ASTNode step;  // may be null

    public SliceNode(ASTNode target, ASTNode start, ASTNode end, ASTNode step) {
        this.target = target;
        this.start = start;
        this.end = end;
        this.step = step;
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

