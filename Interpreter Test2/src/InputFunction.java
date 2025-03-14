import java.util.List;
import javax.swing.SwingUtilities;

public class InputFunction implements Callable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        final String prompt = (!arguments.isEmpty()) ? arguments.get(0).toString() : "";
        try {
            SwingUtilities.invokeAndWait(() -> {
                // Temporarily allow editing.
                Main.consoleArea.setEditable(true);
                // Append the prompt with a trailing space synchronously.
                Main.consoleArea.append(prompt + " ");
                // Set inputStart to mark where user input begins.
                Main.inputStart = Main.consoleArea.getDocument().getLength();
            });
        } catch (Exception e) {
            throw new RuntimeException("Failed to update prompt", e);
        }
        // Wait for the user to press Enter.
        synchronized (Main.inputLock) {
            try {
                Main.inputLock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Input interrupted", e);
            }
        }
        String consoleText = Main.consoleArea.getText();
        String userInput = consoleText.substring(Main.inputStart).trim();
        // Optionally, after capturing input, you can disable editing again.
        SwingUtilities.invokeLater(() -> Main.consoleArea.setEditable(false));
        return userInput;
    }
}
