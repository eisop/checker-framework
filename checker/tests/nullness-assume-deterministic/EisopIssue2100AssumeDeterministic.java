import org.checkerframework.checker.nullness.qual.Nullable;

// With only -AassumeDeterministic, a method might still have side effects, so the result of a
// repeated call is not refined.
public class EisopIssue2100AssumeDeterministic {
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
