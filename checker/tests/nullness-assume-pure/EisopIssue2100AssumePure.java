import org.checkerframework.checker.nullness.qual.Nullable;

// With -AassumeSideEffectFree and -AassumeDeterministic, every method is assumed pure, so the
// result of a repeated call is refined.
public class EisopIssue2100AssumePure {
    @Nullable Object f = new Object();

    @Nullable Object get() {
        return this.f;
    }

    void test() {
        if (get() != null) {
            get().hashCode();
        }
    }
}
