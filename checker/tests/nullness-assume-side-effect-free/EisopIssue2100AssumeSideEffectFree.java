import org.checkerframework.checker.nullness.qual.Nullable;

// With only -AassumeSideEffectFree, a method might still be nondeterministic, so the result of a
// repeated call is not refined.
public class EisopIssue2100AssumeSideEffectFree {
    @Nullable Object f = new Object();

    @Nullable Object get() {
        return this.f;
    }

    void test() {
        if (get() != null) {
            // :: error: (dereference.of.nullable)
            get().hashCode();
        }
    }
}
