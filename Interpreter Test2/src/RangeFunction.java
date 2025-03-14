import java.util.List;
import java.util.ArrayList;
// Define an interface for callable functions.

// Built-in range function: range(n) returns a list of numbers from 0 to n-1.
public class RangeFunction implements Callable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (arguments.size() != 1) {
            throw new RuntimeException("range() expects exactly one argument");
        }
        Object arg = arguments.get(0);
        if (!(arg instanceof Double)) {
            throw new RuntimeException("range() argument must be a number");
        }
        int end = ((Double) arg).intValue();
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            list.add((double) i);
        }
        return list;
    }
}
