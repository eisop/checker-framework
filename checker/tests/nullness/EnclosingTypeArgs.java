import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Test that type arguments of an explicitly-written enclosing type are checked against the
// enclosing type parameter's declared bound, just like the arguments of a direct (non-enclosing)
// parameterized type.  See https://github.com/eisop/checker-framework/issues/737.
class EnclosingTypeArgs {

    static class Min<XXX extends @NonNull Object> {
        class Inner {}
    }

    // Direct (non-enclosing) position: out-of-bound argument is rejected.
    // :: error: (type.argument.type.incompatible)
    void direct(Min<@Nullable String> p) {}

    // Enclosing position: the same out-of-bound argument must also be rejected.
    // :: error: (type.argument.type.incompatible)
    void enclosing(Min<@Nullable String>.Inner p) {}

    // In-bound enclosing argument: no error.
    void okEnclosing(Min<@NonNull String>.Inner p) {}

    // Direct in-bound argument: no error.
    void okDirect(Min<@NonNull String> p) {}
}
