import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FlowFbc {

    @NonNull String f;
    @NotOnlyInitialized @NonNull String g;

    public FlowFbc(String arg) {
        // :: error: (dereference.of.nullable)
        f.toLowerCase();

        // We get a dereference.of.nullable error by the Nullness Checker because g may be null,
        // as well as a method.invocation.invalid error by the Initialization Checker because g
        // is declared as @NotOnlyInitialized and thus may not be @Initialized,
        // but toLowerCase()'s receiver type is, by default, @Initialized.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        g.toLowerCase();

        f = arg;
        g = arg;
        foo();
        f.toLowerCase();
        // :: error: (method.invocation.invalid)
        g.toLowerCase();
        f = arg;
    }

    void test() {
        @Nullable String s = null;
        s = "a";
        s.toLowerCase();
    }

    void test2(@Nullable String s) {
        if (s != null) {
            s.toLowerCase();
        }
    }

    void foo(@UnknownInitialization FlowFbc this) {}

    // TODO Pure, etc.
}
