import java.util.List;
import java.util.ArrayList;

public class Interpreter {

    Environment globals;
    Environment environment;

    // Constructor: initialize global environment and add built-in functions.
    public Interpreter() {
        globals = new Environment(null);
        // Add built-in functions to the global environment.
        globals.define("range", new RangeFunction());
        globals.define("input", new InputFunction());
        globals.define("int", new IntFunction());
        globals.define("float", new FloatFunction());
        environment = globals;
    }
    // Helper function to show errors
    private RuntimeException runtimeError(ASTNode node, String message) {
        return new RuntimeException("Runtime error at line " + node.line + ": " + message);
    }
    // Start of interpreting the ASTNodes from the parsing process, checks each ASTNode type
    public Object evaluate(ASTNode node) {
        if (node instanceof NumberNode) return evaluateNumber((NumberNode) node);
        if (node instanceof StringNode) return evaluateString((StringNode) node);
        if (node instanceof BooleanNode) return evaluateBoolean((BooleanNode) node);
        if (node instanceof IdentifierNode) return evaluateIdentifier((IdentifierNode) node);
        if (node instanceof BinaryOpNode) return evaluateBinaryOp((BinaryOpNode) node);
        if (node instanceof UnaryOpNode) return evaluateUnaryOp((UnaryOpNode) node);
        if (node instanceof AssignmentNode) return evaluateAssignment((AssignmentNode) node);
        if (node instanceof IndexAssignmentNode) return evaluateIndexAssignment((IndexAssignmentNode) node);
        if (node instanceof FieldAssignmentNode) return evaluateFieldAssignment((FieldAssignmentNode) node);
        if (node instanceof PrintNode) return evaluatePrint((PrintNode) node);
        if (node instanceof ReturnNode) return evaluateReturn((ReturnNode) node);
        if (node instanceof BlockNode) return evaluateBlock((BlockNode) node);
        if (node instanceof IfNode) return evaluateIf((IfNode) node);
        if (node instanceof WhileNode) return evaluateWhile((WhileNode) node);
        if (node instanceof ForEachNode) return evaluateForEach((ForEachNode) node);
        if (node instanceof ForNode) return evaluateFor((ForNode) node);
        if (node instanceof FunctionDefinitionNode) return evaluateFunctionDefinition((FunctionDefinitionNode) node);
        if (node instanceof FunctionCallNode) return evaluateFunctionCall((FunctionCallNode) node);
        if (node instanceof ClassDefinitionNode) return evaluateClassDefinition((ClassDefinitionNode) node);
        if (node instanceof ObjectCreationNode) return evaluateObjectCreation((ObjectCreationNode) node);
        if (node instanceof MethodCallNode) return evaluateMethodCall((MethodCallNode) node);
        if (node instanceof ListNode) return evaluateList((ListNode) node);
        if (node instanceof IndexNode) return evaluateIndex((IndexNode) node);
        if (node instanceof SliceNode) return evaluateSlice((SliceNode) node);
        throw new RuntimeException("Unknown AST node type: " + node.getClass().getName());
    }

    // Helper method to return a formatted string for a value
    // For numbers, if the value is mathematically an integer, it omits the trailing .0
    private String formatValue(Object value) {
        if (value instanceof Double) {
            double d = (Double) value;
            if (d == (int) d) {
                return Integer.toString((int) d);
            } else {
                return Double.toString(d);
            }
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                sb.append(formatValue(list.get(i)));
                if (i < list.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
        return String.valueOf(value);
    }
    // Evaluate the tree for the List
    private Object evaluateList(ListNode node) {
        List<Object> list = new ArrayList<>();
        for (ASTNode element : node.getElements()) {
            list.add(evaluate(element));
        }
        return list;
    }
    // Evaluate the indexing of lists
    private Object evaluateIndex(IndexNode node) {
        Object base = evaluate(node.getBase());
        Object index = evaluate(node.getIndex());

        if (!(base instanceof List)) {
            throw runtimeError(node, "Indexing operator can only be applied to lists.");
        }
        List<?> list = (List<?>) base;

        if (!(index instanceof Number)) {
            throw runtimeError(node, "List index must be a number.");
        }
        int idx = ((Number) index).intValue();

        if (idx < 0 || idx >= list.size()) {
            throw runtimeError(node, "List index out of bounds: " + idx);
        }
        return list.get(idx);
    }
    // Evaluate slicing of list
    private Object evaluateSlice(SliceNode node) {
        Object baseObj = evaluate(node.getTarget());
        if (!(baseObj instanceof List)) {
            throw runtimeError(node, "Slice operator can only be applied to lists.");
        }
        List<Object> list = (List<Object>) baseObj;
        int size = list.size();

        // Evaluate start, if missing, default to start is 0.
        int start = 0;
        if (node.getStart() != null) {
            Object startVal = evaluate(node.getStart());
            if (!(startVal instanceof Number)) {
                throw runtimeError(node, "Slice start must be a number.");
            }
            start = ((Number) startVal).intValue();
        }

        // Evaluate end, if missing, default to the list size.
        int end = size;
        if (node.getEnd() != null) {
            Object endVal = evaluate(node.getEnd());
            if (!(endVal instanceof Number)) {
                throw runtimeError(node, "Slice end must be a number.");
            }
            end = ((Number) endVal).intValue();
        }

        // Evaluate step, if missing, default step is 1.
        int step = 1;
        if (node.getStep() != null) {
            Object stepVal = evaluate(node.getStep());
            if (!(stepVal instanceof Number)) {
                throw runtimeError(node, "Slice step must be a number.");
            }
            step = ((Number) stepVal).intValue();
            if (step == 0) {
                throw runtimeError(node, "Slice step cannot be zero.");
            }
        }

        // Evaluates negative indices.
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

    // Modified evaluateIndexAssignment to support both list index assignments and field assignments.
    private Object evaluateIndexAssignment(IndexAssignmentNode node) {
        ASTNode targetExpr = node.getTarget();
        if (targetExpr instanceof IndexNode) {
            IndexNode indexNode = (IndexNode) targetExpr;
            // Evaluate the base expression
            Object base = evaluate(indexNode.getBase());
            if (!(base instanceof List)) {
                throw runtimeError(node, "Index assignment target must be a list.");
            }
            List<Object> list = (List<Object>) base;
            // Evaluate the index expression.
            Object indexVal = evaluate(indexNode.getIndex());
            if (!(indexVal instanceof Number)) { // Accept any Number type.
                throw runtimeError(node, "List index must be a number.");
            }
            int idx = ((Number) indexVal).intValue();
            if (idx < 0 || idx >= list.size()) {
                throw runtimeError(node, "List index out of bounds: " + idx);
            }
            // Evaluate the right-hand side (value to assign).
            Object value = evaluate(node.getValue());
            // Perform the assignment.
            list.set(idx, value);
            return value;
        } else if (targetExpr instanceof FieldAccessNode) {
            // Compound assignment on a field access.
            FieldAccessNode fieldAccess = (FieldAccessNode) targetExpr;
            Object instanceObj = evaluate(fieldAccess.target);
            if (!(instanceObj instanceof Instance)) {
                throw runtimeError(node, "Field assignment target is not an instance.");
            }
            Instance instance = (Instance) instanceObj;
            Object value = evaluate(node.getValue());
            instance.set(fieldAccess.fieldName.value, value);
            return value;
        } else {
            throw runtimeError(node, "Invalid assignment target for compound assignment.");
        }
    }

    // Handle explicit field assignment AST nodes.
    private Object evaluateFieldAssignment(FieldAssignmentNode node) {
        Object targetObj = evaluate(node.target);
        if (!(targetObj instanceof Instance)) {
            throw runtimeError(node, "Field assignment target is not an instance.");
        }
        Instance instance = (Instance) targetObj;
        Object value = evaluate(node.value);
        instance.set(node.fieldName.value, value);
        return value;
    }

    // Evaluates Number Nodes
    private Object evaluateNumber(NumberNode node) {
        double value = Double.parseDouble(node.token.value);
        if (value == (int) value) {
            return (int) value;
        }
        return value;
    }
    // Evaluates Strings
    private Object evaluateString(StringNode node) {
        return node.token.value;
    }

    // Evaluate a boolean literal.
    private Object evaluateBoolean(BooleanNode node) {
        return Boolean.parseBoolean(node.token.value);
    }
    // Evaluates identifiers (variables)
    private Object evaluateIdentifier(IdentifierNode node) {
        // If inside a method, check instance attributes first.
        if (environment.exists("this")) {
            Object thisVal = environment.get("this", node.line);
            if (thisVal instanceof Instance) {
                Instance instance = (Instance) thisVal;
                // Check if the instance has a field with the given name.
                if (instance.hasField(node.identifier.value)) {
                    return instance.get(node.identifier.value);
                }
            }
        }
        // Otherwise, go back to check the scope before in the environment lookup.
        return environment.get(node.identifier.value, node.line);
    }
    // Evaluates equals
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    // Evaluates Binary Operations
    private Object evaluateBinaryOp(BinaryOpNode node) {
        Object left = evaluate(node.left);
        Object right = evaluate(node.right);
        String op = node.op.value;

        // Equality operators
        if (op.equals("==")) {
            return isEqual(left, right);
        }
        if (op.equals("!=")) {
            return !isEqual(left, right);
        }

        // Membership test: if x in list.
        if (op.equals("in")) {
            if (!(right instanceof List)) {
                throw runtimeError(node, "Operator 'in' expects a list as the right operand.");
            }
            List<?> list = (List<?>) right;
            return list.contains(left);
        }

        // Logical operators and, or.
        if (op.equals("and")) {
            return isTruthy(left) && isTruthy(right);
        }
        if (op.equals("or")) {
            return isTruthy(left) || isTruthy(right);
        }

        // String concatenation when one operand is a string.
        if (left instanceof String || right instanceof String) {
            if (op.equals("+")) {
                return formatValue(left) + formatValue(right);
            } else {
                throw runtimeError(node, "Unsupported operation for strings: " + op);
            }
        }

        // Numeric operations.
        if (left instanceof Number && right instanceof Number) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/":
                    if (r == 0) {
                        throw runtimeError(node, "Division by 0");
                    }
                    return l/r;
                case "%": return l % r;
                case ">": return l > r;
                case ">=": return l >= r;
                case "<": return l < r;
                case "<=": return l <= r;
                default:
                    throw runtimeError(node, "Unknown binary operator: " + op);
            }
        }
        throw runtimeError(node, "Unsupported operands for operator '" + op + "': " +
                left.getClass().getSimpleName() + " and " + right.getClass().getSimpleName());
    }
    // Evaluates Unary Operations
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
                throw runtimeError(node, "Unknown unary operator: " + op);
        }
    }
    // Evaluates assignments (For example variable assignment or attribute assignments)
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

    // Evaluate prints
    private Object evaluatePrint(PrintNode node) {
        Object value = evaluate(node.expression);
        System.out.println(formatValue(value));
        return null;
    }
    // Evaluates return (from function)
    private Object evaluateReturn(ReturnNode node) {
        Object value = evaluate(node.expression);
        throw new Return(value);
    }
    // Evaluates block nodes
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

    // Helper method: evaluates a block using an existing environment (without creating a new one).
    private Object evaluateBlockNoNewEnv(BlockNode node, Environment env) {
        Object result = null;
        Environment previous = environment;
        environment = env;
        for (ASTNode statement : node.statements) {
            result = evaluate(statement);
        }
        environment = previous;
        return result;
    }

    // Handle else-if by recursively evaluating the else branch.
    private Object evaluateIf(IfNode node) {
        Object condition = evaluate(node.condition);
        if (isTruthy(condition)) {
            return evaluate(node.thenBranch);
        } else if (node.elseBranch != null) {
            // The else branch might be an "else if" (represented as a nested IfNode) or a plain else
            return evaluate(node.elseBranch);
        }
        return null;
    }

    // Evaluate a while loop.
    private Object evaluateWhile(WhileNode node) {
        Object result = null;
        while (isTruthy(evaluate(node.condition))) {
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

    // Evaluate a for loop (numeric variant: for i = 1,3)
    private Object evaluateFor(ForNode node) {
        // Evaluate the start and end expressions
        Object startObj = evaluate(node.start);
        Object endObj = evaluate(node.end);
        if (!(startObj instanceof Number) || !(endObj instanceof Number)) {
            throw runtimeError(node, "For loop: start and end values must be numbers.");
        }
        Number startVal = (Number) startObj;
        Number endVal = (Number) endObj;
        Object result = null;

        // Ensure the loop variable is defined in the current environment
        if (!environment.containsLocally(node.loopVar.value)) {
            environment.define(node.loopVar.value, startVal);
        }

        // Iterate from the start value to the end value
        for (int i = (int) startVal; i <= (int) endVal; i++) {
            environment.assign(node.loopVar.value, (double) i);
            result = evaluate(node.body);
        }
        return result;
    }

    // Evaluate a for-each loop (for i in x)
    private Object evaluateForEach(ForEachNode node) {
        Object iterable = evaluate(node.getListExpr());
        if (!(iterable instanceof List)) {
            throw runtimeError(node, "For-each loop expects a list after 'in'.");
        }
        List<?> list = (List<?>) iterable;
        Object result = null;
        // Create a persistent loop environment that will persist across iterations.
        Environment loopEnv = new Environment(environment);
        // Define the loop variable in the loop environment.
        loopEnv.define(node.getLoopVar().value, null);
        for (Object element : list) {
            loopEnv.assign(node.getLoopVar().value, element);
            // Evaluate the loop body in the persistent loop environment.
            if (node.getBody() instanceof BlockNode) {
                result = evaluateBlockNoNewEnv((BlockNode) node.getBody(), loopEnv);
            } else {
                Environment previous = environment;
                environment = loopEnv;
                result = evaluate(node.getBody());
                environment = previous;
            }
        }
        return result;
    }
    // Evaluates function definitions
    private Object evaluateFunctionDefinition(FunctionDefinitionNode node) {
        Function function = new Function(node, environment);
        environment.define(node.name.value, function);
        return function;
    }

    // Evaluates function calls
    private Object evaluateFunctionCall(FunctionCallNode node) {
        Object callee = environment.get(node.name.value);
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        if (callee instanceof Function) {
            Function function = (Function) callee;
            if (arguments.size() != function.declaration.parameters.size()) {
                throw runtimeError(node, "Function " + node.name.value + " expects " +
                        function.declaration.parameters.size() + " arguments, but got " + arguments.size());
            }
            try {
                return function.call(this, arguments);
            } catch (Return r) {
                return r.value;
            }
        } else if (callee instanceof Callable) {
            Callable callable = (Callable) callee;
            return callable.call(this, arguments);
        } else {
            throw runtimeError(node, "Attempted to call a non-function: " + node.name.value);
        }
    }
    // Evaluates class definitions
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
    // Evaluates object creations
    private Object evaluateObjectCreation(ObjectCreationNode node) {
        Object classObj = environment.get(node.className.value);
        if (!(classObj instanceof ClassValue)) {
            throw runtimeError(node, "Attempted to instantiate non-class: " + node.className.value);
        }
        ClassValue classValue = (ClassValue) classObj;
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        // Get the initializer method (if any)
        Function initMethod = classValue.findMethod("init");
        if (initMethod == null && !arguments.isEmpty()) {
            throw runtimeError(node, "Constructor arguments provided, but no initializer ('init') defined for class: " + classValue.name);
        }
        if (initMethod != null) {
            int expectedParamCount = initMethod.paramCount();
            if (arguments.size() != expectedParamCount) {
                throw runtimeError(node, "Initializer 'init' for class " + classValue.name + " expects " + expectedParamCount + " argument(s), but received " + arguments.size());
            }
        }
        return classValue.instantiate(arguments, this);
    }

    // Evaluate method calls
    private Object evaluateMethodCall(MethodCallNode node) {
        Object target = evaluate(node.target);
        // Check if the target is a list and the method is a built-in list method.
        if (target instanceof List) {
            String methodName = node.methodName.value;
            List<Object> list = (List<Object>) target;
            if (methodName.equals("append")) {
                if (node.arguments.size() != 1) {
                    throw runtimeError(node, "append() expects one argument.");
                }
                Object arg = evaluate(node.arguments.getFirst());
                list.add(arg);
                return null;
            } else if (methodName.equals("pop")) {
                if (!node.arguments.isEmpty()) {
                    throw runtimeError(node, "pop() expects no arguments.");
                }
                if (list.isEmpty()) {
                    throw runtimeError(node, "pop() called on an empty list.");
                }
                return list.removeLast();
            } else if (methodName.equals("remove")) {
                // remove(item): removes the first occurrence of item.
                if (node.arguments.size() != 1) {
                    throw runtimeError(node, "remove() expects one argument.");
                }
                Object arg = evaluate(node.arguments.getFirst());
                boolean removed = list.remove(arg); // removes first occurrence if found.
                if (!removed) {
                    throw runtimeError(node, "remove() did not find the element to remove: " + arg);
                }
                return null;
            } else if (methodName.equals("size")) {
                // size() returns the number of elements in the list.
                if (!node.arguments.isEmpty()) {
                    throw runtimeError(node, "size() expects no arguments.");
                }
                return (double) list.size();
            }
        }
        // Otherwise, handle it as a normal instance method call.
        if (!(target instanceof Instance)) {
            throw runtimeError(node, "Attempted to call method on non-instance.");
        }
        Instance instance = (Instance) target;
        List<Object> arguments = new ArrayList<>();
        for (ASTNode arg : node.arguments) {
            arguments.add(evaluate(arg));
        }
        Function method = instance.getMethod(node.methodName.value);
        if (method == null) {
            throw runtimeError(node, "Method '" + node.methodName.value + "' not found.");
        }
        try {
            return method.call(this, arguments, instance);
        } catch (Return r) {
            return r.value;
        }
    }
    // Evaluate booleans
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
