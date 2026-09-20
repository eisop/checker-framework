import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * If you run:
 *
 * <pre>bazel build example</pre>
 *
 * then the build for this project should fail with a warning for the line:
 *
 * <pre>@NonNull Object nn = nullable;</pre>
 */
public class BazelExample {

    public static @Nullable Object nullable = null;
    public Map<Object, Object> map = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Hello World!");

        @NonNull Object nn = null; // error on this line
        System.out.println(nn.hashCode()); // NPE
    }

    // Test for -J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED.
    void mapTest(@KeyFor("map") Object k) {}
}
