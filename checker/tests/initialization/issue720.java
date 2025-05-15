// test case for issue 720
// https://github.com/eisop/checker-framework/issues/720

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;

class Issue720 {
    @NotOnlyInitialized @NonNull Issue720Field f = new Issue720Field();

    Issue720() {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @Under/@UnknownInitialization.
        foo1();
        foo2();
    }

    void foo1(@UnderInitialization Issue720 this) {
        // :: error: (dereference.of.nullable)
        f.FieldAccess1();
    }

    void foo2(@UnknownInitialization Issue720 this) {
        // :: error: (dereference.of.nullable)
        f.FieldAccess1();
    }

    void foo3() {
        // @NotOnlyInitialized should be correctly adapted to @Initialized by @Initialized.
        // This is the only way to enter then branch in the issue. The correct adaption ensures the
        // correct use of @NotOnlyInitialized.
        foo4();
    }

    void foo4() {
        f.FieldAccess2();
    }
}

class Issue720Field {
    @NotOnlyInitialized Object o = new Object();

    void FieldAccess1(@UnknownInitialization Issue720Field this) {}

    void FieldAccess2() {}
}
