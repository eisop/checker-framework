import org.checkerframework.checker.nullness.qual.Nullable;

class EisopIssue635 {

    private @Nullable Runnable r;

    private void f() {
        // No crash without this assignment first.
        r = null;
        r =
                new Runnable() {
                    @Override
                    public void run() {
                        if (r != this) {
                            return;
                        }
                        // No crash without this call.
                        f();
                    }
                };
    }
}
