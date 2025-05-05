import org.checkerframework.checker.nullness.qual.Nullable;

public class SkipDirs2 {
    static class SkipMe {

        Object f;

        // If this test is NOT skipped, it should issue an "unexpected error" since
        // there is a type error between f2 (Nullable) and f (NonNull).
        void foo(@Nullable Object f2) {
            f = f2;
        }
    }
}
