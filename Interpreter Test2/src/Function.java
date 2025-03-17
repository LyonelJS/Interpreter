import java.util.List;
import java.util.ArrayList;

public class Function {
    public final FunctionDefinitionNode declaration;
    private final Environment closure;

    public Function(FunctionDefinitionNode declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = new Environment(closure);
    }
    public int paramCount() {
        return declaration.parameters.size();
    }
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        List<Token> params = declaration.parameters;
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).value, arguments.get(i));
        }
        try {
            interpreter.executeBlock(((BlockNode) declaration.body).statements, environment);
        } catch (Return r) {
            return r.value;
        }
        return null;
    }

    // For method calls: bind "this"
    public Object call(Interpreter interpreter, List<Object> arguments, Instance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        List<Token> params = declaration.parameters;
        for (int i = 0; i < params.size(); i++) {
            environment.define(params.get(i).value, arguments.get(i));
        }
        try {
            interpreter.executeBlock(((BlockNode) declaration.body).statements, environment);
        } catch (Return r) {
            return r.value;
        }
        return null;
    }
}
