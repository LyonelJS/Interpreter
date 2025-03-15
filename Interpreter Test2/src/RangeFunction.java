import java.util.List;
import java.util.ArrayList;

// Built-in range function: supports range(n), range(start, end), and range(start, end, step).
public class RangeFunction implements Callable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        int start, end, step;

        if (arguments.size() == 1) {
            // range(n): start=0, end=n, step=1.
            if (!(arguments.get(0) instanceof Number)) {
                throw new RuntimeException("range() argument must be a number");
            }
            start = 0;
            end = ((Number) arguments.get(0)).intValue();
            step = 1;
        } else if (arguments.size() == 2) {
            // range(start, end): step=1.
            if (!(arguments.get(0) instanceof Number && arguments.get(1) instanceof Number)) {
                throw new RuntimeException("range() arguments must be numbers");
            }
            start = ((Number) arguments.get(0)).intValue();
            end = ((Number) arguments.get(1)).intValue();
            step = 1;
        } else if (arguments.size() == 3) {
            // range(start, end, step)
            if (!(arguments.get(0) instanceof Number && arguments.get(1) instanceof Number
                    && arguments.get(2) instanceof Number)) {
                throw new RuntimeException("range() arguments must be numbers");
            }
            start = ((Number) arguments.get(0)).intValue();
            end = ((Number) arguments.get(1)).intValue();
            step = ((Number) arguments.get(2)).intValue();
            if (step == 0) {
                throw new RuntimeException("range() step argument must not be zero");
            }
        } else {
            throw new RuntimeException("range() expects 1, 2, or 3 arguments");
        }

        List<Object> list = new ArrayList<>();
        if (step > 0) {
            for (int i = start; i < end; i += step) {
                list.add(i);
            }
        } else {
            // For negative steps, count downwards.
            for (int i = start; i > end; i += step) {
                list.add(i);
            }
        }
        return list;
    }
}
