import org.checkerframework.checker.initialization.qual.UnknownInitialization;

// This tests that the prefix "initialization" suppresses warnings by the Initialization Checker.
// The test case nullness-initialization/Suppression covers the prefixes used by the
// Nullness Checker.

public class Suppression {

    Suppression t;

    @SuppressWarnings("initialization.fields.uninitialized")
    public Suppression(Suppression arg) {}

    @SuppressWarnings({"initialization"})
    void foo(@UnknownInitialization Suppression arg) {
        t = arg; // initialization error
    }
}
