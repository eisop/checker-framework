import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Refining an operand of == by the other operand's value, where one of them is a use of a type
// variable without a primary annotation.
public class EqualToTypeVariable<T> {
    @MonotonicNonNull T f;

    void sameTypeVariable(T b) {
        if (f == b) {
            // f may still be null, so b may be null.
            // :: error: (dereference.of.nullable)
            b.toString();
        }
    }

    <S, U extends @NonNull Object> void differentTypeVariables(S s, @Nullable U u) {
        if (u == s) {
            // s may be null, so u may be null.
            // :: error: (dereference.of.nullable)
            u.toString();
        }
    }

    void refinedToNonNull(@NonNull T a, T b) {
        if (a == b) {
            b.toString();
        }
    }
}
