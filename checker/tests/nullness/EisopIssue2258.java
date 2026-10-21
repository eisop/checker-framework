import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Test that the type arguments of every enclosing type of a local variable's type are checked
// against their declared bounds.  See https://github.com/eisop/checker-framework/issues/2258.
class EisopIssue2258 {
    static class A<XXX extends @NonNull Object> {
        class B<YYY extends @NonNull Object> {
            class C {}
        }
    }

    // :: error: (type.argument.type.incompatible)
    void param(A<@Nullable String>.B<@NonNull String>.C p) {}

    void local() {
        // :: error: (type.argument.type.incompatible)
        A<@Nullable String>.B<@NonNull String>.C c = null;
        // :: error: (type.argument.type.incompatible)
        A<@NonNull String>.B<@Nullable String>.C d = null;
        A<@NonNull String>.B<@NonNull String>.C ok = null;
    }
}
