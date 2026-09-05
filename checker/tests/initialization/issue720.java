// test case for issue 720
// https://github.com/eisop/checker-framework/issues/720

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class Issue720 {
    @NotOnlyInitialized Object f = new Object();

    void fieldAccess1(@UnderInitialization Issue720 this) {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @UnderInitialization.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        f.hashCode();
    }

    void fieldAccess2(@UnknownInitialization Issue720 this) {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @UnknownInitialization.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        f.hashCode();
    }

    void fieldAccess3() {
        // @NotOnlyInitialized should be correctly adapted to @Initialized by @Initialized.
        // This is the only way to enter then branch in the issue. The correct adaption ensures the
        // correct use of @NotOnlyInitialized.
        f.hashCode();
    }

    // False positive: The initializer should be consistent with constructor.
    // The LHS should be adapted to @UnknownInitialization instead of Initialized.
    // :: error: (assignment.type.incompatible)
    @NotOnlyInitialized Object g = this;
    @NotOnlyInitialized Object h;

    Issue720() {
        h = this;
    }
}
