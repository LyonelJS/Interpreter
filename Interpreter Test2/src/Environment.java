import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(String name, Object value) {
        values.put(name, value);
    }

    public Object get(String name, int line) {
        if (values.containsKey(name)) {
            return values.get(name);
        }
        if (enclosing != null) {
            return enclosing.get(name, line);
        }
        throw new RuntimeException("Undefined variable at line " + line + ": " + name);
    }

    public Object get(String name) {
        return get(name, -1); // or choose to not use it at all.
    }

    public boolean exists(String name) {
        if (values.containsKey(name)) return true;
        if (enclosing != null) return enclosing.exists(name);
        return false;
    }

    public boolean containsLocally(String name) {
        return values.containsKey(name);
    }

    public void assign(String name, Object value, int line) {
        if (values.containsKey(name)) {
            values.put(name, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value, line);
            return;
        }
        throw new RuntimeException("Undefined variable at line " + line + ": " + name);
    }

    public void assign(String name, Object value) {
        assign(name, value, -1);
    }
}
