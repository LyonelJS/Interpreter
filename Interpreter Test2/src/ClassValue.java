import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ClassValue {
    public final String name;
    private final Map<String, Function> methods = new HashMap<>();
    public final Map<String, Object> fields = new HashMap<>(); // Public for instance initialization.

    public ClassValue(String name) {
        this.name = name;
    }

    public void defineMethod(String name, Function method) {
        methods.put(name, method);
    }

    public void defineField(String name, Object value) {
        fields.put(name, value);
    }

    public Instance instantiate(List<Object> arguments, Interpreter interpreter) {
        Instance instance = new Instance(this);
        // Require an initializer to assign the class attributes
        Function initializer = methods.get("init");
        if (!arguments.isEmpty() && initializer == null) {
            throw new RuntimeException("Constructor arguments provided, but no initializer ('init') defined for class: " + name);
        }
        if (initializer != null) {
            initializer.call(interpreter, arguments, instance);
        }
        return instance;
    }

    public Function findMethod(String name) {
        return methods.get(name);
    }
}
