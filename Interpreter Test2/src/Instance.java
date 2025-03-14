public class Instance {
    private final ClassValue klass;
    private final Environment fields;

    public Instance(ClassValue klass) {
        this.klass = klass;
        this.fields = new Environment(null);
        // Initialize fields with class default values.
        for (String key : klass.fields.keySet()) {
            fields.define(key, klass.fields.get(key));
        }
    }

    public Object get(String name) {
        // Use containsLocally instead of containsKey.
        if (fields.containsLocally(name)) {
            return fields.get(name);
        }
        Function method = klass.findMethod(name);
        if (method != null) return method;
        throw new RuntimeException("Undefined property '" + name + "'.");
    }

    public void set(String name, Object value) {
        if (fields.containsLocally(name)) {
            fields.assign(name, value);
        } else {
            fields.define(name, value);
        }
    }

    public boolean hasField(String name) {
        // Use the correct Environment method.
        return fields.containsLocally(name);
    }

    public Function getMethod(String name) {
        return klass.findMethod(name);
    }
}
