import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** The expected error key of a test can be written in square brackets or in parentheses. */
public class BracketErrorKeys {
    void m(@Nullable Object o) {
        // :: error: [assignment.type.incompatible]
        @NonNull Object inBrackets = o;
        // :: error: (assignment.type.incompatible)
        @NonNull Object inParentheses = o;
        // :: error: assignment.type.incompatible
        @NonNull Object bare = o;
    }
}
