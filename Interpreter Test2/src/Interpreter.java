import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Interpreter {

    Environment globals;
    Environment environment;

    // Constructor: initialize global environment and add built-in functions.
    public Interpreter() {
        globals = new Environment(null);
        // Add built-in range function to the global environment.
        globals.define("range", new RangeFunction());
        environment = globals;
    }

    public void interpret(ASTNode node) {
        try {
            evaluate(node);
        } catch (RuntimeException e) {
            System.out.println("Runtime error: " + e.getMessage());
        }
    }

    public Object evaluate(ASTNode node) {
        if (node instanceof NumberNode) return evaluateNumber((NumberNode) node);
        if (node instanceof StringNode) return evaluateString((StringNode) node);
        if (node instanceof BooleanNode) return evaluateBoolean((BooleanNode) node);
        if (node instanceof IdentifierNode) return evaluateIdentifier((IdentifierNode) node);
        if (node instanceof BinaryOpNode) return evaluateBinaryOp((BinaryOpNode) node);
        if (node instanceof UnaryOpNode) return evaluateUnaryOp((UnaryOpNode) node);
        if (node instanceof AssignmentNode) return evaluateAssignment((AssignmentNode) node);
        if (node instanceof PrintNode) return evaluatePrint((PrintNode) node);
        if (node instanceof ReturnNode) return evaluateReturn((ReturnNode) node);
        if (node instanceof BlockNode) return evaluateBlock((BlockNode) node);
        if (node instanceof IfNode) return evaluateIf((IfNode) node);
        if (node instanceof WhileNode) return evaluateWhile((WhileNode) node);
        if (node instanceof ForNode) return evaluateFor((ForNode) node);
        if (node instanceof FunctionDefinitionNode) return evaluateFunctionDefinition((FunctionDefinitionNode) node);
        if (node instanceof FunctionCallNode) return evaluateFunctionCall((FunctionCallNode) node);
        if (node instanceof ClassDefinitionNode) return evaluateClassDefinition((ClassDefinitionNode) node);
        if (node instanceof ObjectCreationNode) return evaluateObjectCreation((ObjectCreationNode) node);
        if (node instanceof MethodCallNode) return evaluateMethodCall((MethodCallNode) node);
        if (node instanceof ListNode) return evaluateList((ListNode) node);
        if (node instanceof IndexNode) return evaluateIndex((IndexNode) node);
        if (node instanceof IndexAssignmentNode) return evaluateIndexAssignment((IndexAssignmentNode) node);
        if (node instanceof SliceNode) return evaluateSlice((SliceNode) node);
        throw new RuntimeException("Unknown AST node type: " + node.getClass().getName());
    }
    private Object evaluateList(ListNode node) {
        List<Object> list = new ArrayList<>();
        for (ASTNode element : node.getElements()) {
            list.add(evaluate(element));
        }
        return list;
    }
    private Object evaluateIndex(IndexNode node) {
        Object base = evaluate(node.getBase());
        Object index = evaluate(node.getIndex());

        if (!(base instanceof List)) {
            throw new RuntimeException("Indexing operator can only be applied to lists.");
        }
        List<?> list = (List<?>) base;

        if (!(index instanceof Double)) { // Assuming numbers are represented as Double
            throw new RuntimeException("List index must be a number.");
        }
        int idx = ((Double) index).intValue();

        if (idx < 0 || idx >= list.size()) {
            throw new RuntimeException("List index out of bounds: " + idx);
        }
        return list.get(idx);
    }
    private Object evaluateSlice(SliceNode node) {
        Object baseObj = evaluate(node.getTarget());
        if (!(baseObj instanceof List)) {
            throw new RuntimeException("Slice operator can only be applied to lists.");
        }
        List<Object> list = (List<Object>) baseObj;
        int size = list.size();

        // Evaluate start; if missing, default to 0.
        int start = 0;
        if (node.getStart() != null) {
            Object startVal = evaluate(node.getStart());
            if (!(startVal instanceof Double)) {
                throw new RuntimeException("Slice start must be a number.");
            }
            start = ((Double) startVal).intValue();
        }

        // Evaluate end; if missing, default to size.
        int end = size;
        if (node.getEnd() != null) {
            Object endVal = evaluate(node.getEnd());
            if (!(endVal instanceof Double)) {
                throw new RuntimeException("Slice end must be a number.");
            }
            end = ((Double) endVal).intValue();
        }

        // Evaluate step; if missing, default to 1.
        int step = 1;
        if (node.getStep() != null) {
            Object stepVal = evaluate(node.getStep());
            if (!(stepVal instanceof Double)) {
                throw new RuntimeException("Slice step must be a number.");
            }
            step = ((Double) stepVal).intValue();
            if (step == 0) {
                throw new RuntimeException("Slice step cannot be zero.");
            }
        }

        // Normalize negative indices.
        if (start < 0) start = size + start;
        if (end < 0) end = size + end;

        // Clamp indices to valid range.
        if (start < 0) start = 0;
        if (end > size) end = size;

        List<Object> result = new ArrayList<>();
        if (step > 0) {
            for (int i = start; i < end; i += step) {
                result.add(list.get(i));
            }
        } else { // For negative step, iterate in reverse.
            for (int i = start; i > end; i += step) {
                result.add(list.get(i));
            }
        }
        return result;
    }

    private Object evaluateIndexAssignment(IndexAssignmentNode node) {
        // The target should be an IndexNode.
        if (!(node.getTarget() instanceof IndexNode)) {
            throw new RuntimeException("Invalid index assignment target.");
        }
        IndexNode indexNode = (IndexNode) node.getTarget();

        // Evaluate the base expression (should yield a List).
        Object base = evaluate(indexNode.getBase());
        if (!(base instanceof List)) {
            throw new RuntimeException("Index assignment target must be a list.");
        }
        List<Object> list = (List<Object>) base;

        // Evaluate the index expression.
        Object indexVal = evaluate(indexNode.getIndex());
        if (!(indexVal instanceof Double)) { // Assuming numbers are represented as Double.
            throw new RuntimeException("List index must be a number.");
        }
        int idx = ((Double) indexVal).intValue();
        if (idx < 0 || idx >= list.size()) {
            throw new RuntimeException("List index out of bounds: " + idx);
        }

        // Evaluate the right-hand side (value to assign).
        Object value = evaluate(node.getValue());
        // Perform the assignment.
        list.set(idx, value);
        return value;
    }

    private Object evaluateNumber(NumberNode node) {
        return Double.parseDouble(node.token.value);
    }

    private Object evaluateString(StringNode node) {
        return node.token.value;
    }

    // Evaluate a boolean literal.
    private Object evaluateBoolean(BooleanNode node) {
        return Boolean.parseBoolean(node.token.value);
    }

    private Object evaluateIdentifier(IdentifierNode node) {
        // If we're inside a method (i.e. "this" exists), check instance attributes first.
        if (environment.exists("this")) {
            Object thisVal = environment.get("this");
            if (thisVal instanceof Instance) {
                Instance instance = (Instance) thisVal;
                // Check if the instance has a field with the given name.
                if (instance.hasField(node.identifier.value)) {
                    return instance.get(node.identifier.value);
                }
            }
        }
        // Otherwise, fall back to the normal environment lookup.
        return environment.get(node.identifier.value);
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private Object evaluateBinaryOp(BinaryOpNode node) {
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);
        String op = node.op.value;

        // Equality operators (applicable to all types).
        if (op.equals("==")) {
            return isEqual(left, right);
        }
        if (op.equals("!=")) {
            return !isEqual(left, right);
        }

        // Logical operators: and, or.
        if (op.equals("and")) {
            return isTruthy(left) && isTruthy(right);
        }
        if (op.equals("or")) {
            return isTruthy(left) || isTruthy(right);
        }

        // String concatenation when one operand is a string.
        if (left instanceof String || right instanceof String) {
            if (op.equals("+")) {
                return left.toString() + right.toString();
            } else {
                throw new RuntimeException("Unsupported operation for strings: " + op);
            }
        }

        // Numeric operations.
        if (left instanceof Double && right instanceof Double) {
            double l = (Double) left;
            double r = (Double) right;
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/": return l / r;
                case ">": return l > r;
                case ">=": return l >= r;
                case "<": return l < r;
                case "<=": return l <= r;
                default:
                    throw new RuntimeException("Unknown binary operator: " + op);
            }
        }
        throw new RuntimeException("Unsupported operands for operator '" + op + "': " +
                left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName());
    }

    private Object evaluateUnaryOp(UnaryOpNode node) {
        Object operand = evaluate(node.operand);
        String op = node.op.value;
        if (op.equals("not")) {
            return !isTruthy(operand);
        }
        // Assume numeric unary operators.
        double val = (Double) operand;
        switch (op) {
            case "+": return +val;
            case "-": return -val;
            default:
                throw new RuntimeException("Unknown unary operator: " + op);
        }
    }

    private Object evaluateAssignment(AssignmentNode node) {
        Object value = evaluate(node.value);
        if (environment.containsLocally(node.identifier.value)) {
            environment.assign(node.identifier.value, value);
        } else if (environment.exists("this")) {
            Object thisVal = environment.get("this");
            if (thisVal instanceof Instance) {
                ((Instance) thisVal).set(node.identifier.value, value);
            } else {
                environment.define(node.identifier.value, value);
            }
        } else {
            environment.define(node.identifier.value, value);
        }
        return value;
    }

    private Object evaluatePrint(PrintNode node) {
        Object value = evaluate(node.expression);
        System.out.println(value);
        return null;
    }

    private Object evaluateReturn(ReturnNode node) {
        Object value = evaluate(node.expression);
        throw new Return(value);
    }

    private Object evaluateBlock(BlockNode node) {
        Object result = null;
        Environment previous = environment;
        environment = new Environment(previous);
        for (ASTNode statement : node.statements) {
            result = evaluate(statement);
        }
        environment = previous;
        return result;
    }

    private Object evaluateIf(IfNode node) {
        Object condition = evaluate(node.condition);
        if (isTruthy(condition)) {
            return evaluate(node.thenBranch);
        } else if (node.elseBranch != null) {
            return evaluate(node.elseBranch);
        }
        return null;
    }

    // Evaluate a while loop.
    private Object evaluateWhile(WhileNode node) {
        Object result = null;
        while (isTruthy(evaluate(node.condition))) {
            // If the body is a BlockNode, execute its statements in the current environment.
            if (node.body instanceof BlockNode) {
                BlockNode block = (BlockNode) node.body;
                for (ASTNode statement : block.statements) {
                    result = evaluate(statement);
                }
            } else {
                result = evaluate(node.body);
            }
        }
        return result;
    }



    // Evaluate a for loop.
    // Here, we assume that the iterable expression evaluates to a List.
    private Object evaluateFor(ForNode node) {
        // Evaluate the start and end expressions.
        Object startObj = evaluate(node.start);
        Object endObj = evaluate(node.end);
        if (!(startObj instanceof Double) || !(endObj instanceof Double)) {
            throw new RuntimeException("For loop: start and end values must be numbers.");
        }
        double startVal = (Double) startObj;
        double endVal = (Double) endObj;
        Object result = null;

        // Ensure the loop variable is defined in the current environment.
        if (!environment.containsLocally(node.loopVar.value)) {
            environment.define(node.loopVar.value, (double) startVal);
        }

        // Iterate from the start value to the end value (inclusive).
        for (int i = (int) startVal; i <= (int) endVal; i++) {
            environment.assign(node.loopVar.value, (double) i);
            result = evaluate(node.body);
        }
        return result;
    }

    private Object evaluateFunctionDefinition(FunctionDefinitionNode node) {
        Function function = new Function(node, environment);
        environment.define(node.name.value, function);
        return function;
    }

    private Object evaluateFunctionCall(FunctionCallNode node) {
        Object callee = environment.get(node.name.value);
        if (!(callee instanceof Function)) {
            throw new RuntimeException("Attempted to call a non-function: " + node.name.value);
        }
        Function function = (Function) callee;
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        if (arguments.size() != function.declaration.parameters.size()) {
            throw new RuntimeException("Function " + node.name.value + " expects " +
                    function.declaration.parameters.size() + " arguments, but got " + arguments.size());
        }
        try {
            return function.call(this, arguments);
        } catch (Return r) {
            return r.value;
        }
    }

    private Object evaluateClassDefinition(ClassDefinitionNode node) {
        ClassValue classValue = new ClassValue(node.name.value);
        for (ASTNode member : node.members) {
            if (member instanceof FunctionDefinitionNode) {
                Function method = new Function((FunctionDefinitionNode) member, environment);
                classValue.defineMethod(((FunctionDefinitionNode) member).name.value, method);
            } else if (member instanceof AssignmentNode) {
                Object fieldVal = evaluate(member);
                classValue.defineField(((AssignmentNode) member).identifier.value, fieldVal);
            }
        }
        environment.define(node.name.value, classValue);
        return classValue;
    }

    private Object evaluateObjectCreation(ObjectCreationNode node) {
        Object classObj = environment.get(node.className.value);
        if (!(classObj instanceof ClassValue)) {
            throw new RuntimeException("Attempted to instantiate non-class: " + node.className.value);
        }
        ClassValue classValue = (ClassValue) classObj;
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        // If arguments are provided, require an initializer method.
        if (!arguments.isEmpty() && classValue.findMethod("init") == null) {
            throw new RuntimeException("Constructor arguments provided, but no initializer ('init') defined for class: " + classValue.name);
        }
        return classValue.instantiate(arguments, this);
    }

    private Object evaluateMethodCall(MethodCallNode node) {
        Object target = evaluate(node.target);
        if (!(target instanceof Instance)) {
            throw new RuntimeException("Attempted to call method on non-instance.");
        }
        Instance instance = (Instance) target;
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        Function method = instance.getMethod(node.methodName.value);
        if (method == null) {
            throw new RuntimeException("Method '" + node.methodName.value + "' not found.");
        }
        try {
            return method.call(this, arguments, instance);
        } catch (Return r) {
            return r.value;
        }
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return true;
    }

    public void executeBlock(List<ASTNode> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (ASTNode statement : statements) {
                evaluate(statement);
            }
        } finally {
            this.environment = previous;
        }
    }
}
