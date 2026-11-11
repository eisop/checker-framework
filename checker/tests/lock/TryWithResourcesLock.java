import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;

// The CFG copies the close() call of each resource for every exit of a try-with-resources. The
// copies on exits that cannot be taken must not report an error.
public class TryWithResourcesLock {
    static String stackTrace(Throwable t) {
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
