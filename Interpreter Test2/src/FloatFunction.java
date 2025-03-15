import java.util.List;

public class FloatFunction implements Callable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (arguments.isEmpty()) {
            throw new RuntimeException("float() requires at least one argument.");
        }

        Object value = arguments.get(0);

        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid float conversion: " + value);
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            return value; // Already a float
        } else {
            throw new RuntimeException("Cannot convert " + value + " to float.");
        }
    }
}
