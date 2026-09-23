// Test case for Issue 1217:
// https://github.com/eisop/checker-framework/issues/1217
// Viewpoint adaptation of @NotOnlyInitialized field initializers when receiver is under
// initialization.

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

public class Issue1217 {
    // Under initialization receiver (this), @NotOnlyInitialized field initializer
    // is adapted to @UnknownInitialization, consistent with assignment in constructor:
    @NotOnlyInitialized Object f1 = this;

    // Normal field (not @NotOnlyInitialized) retains declared type @Initialized,
    // so assigning 'this' is rejected:
    // :: error: (assignment.type.incompatible)
    Object f2 = this;

    @NotOnlyInitialized Issue1217 f3;
    Issue1217 f4;

    Issue1217() {
        f3 = this;
        // :: error: (assignment.type.incompatible)
        f4 = this;
    }
}
