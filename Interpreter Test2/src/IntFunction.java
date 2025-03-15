import java.util.List;

public class IntFunction implements Callable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (arguments.isEmpty()) {
            throw new RuntimeException("int() requires at least one argument.");
        }

        Object value = arguments.get(0);

        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid integer conversion: " + value);
            }
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        } else if (value instanceof Integer) {
            return value; // Already an int
        } else {
            throw new RuntimeException("Cannot convert " + value + " to int.");
        }
    }
}
