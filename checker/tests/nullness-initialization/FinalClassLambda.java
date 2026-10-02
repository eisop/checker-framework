// Test case for EISOP issues #640 and #641:
// https://github.com/eisop/checker-framework/issues/640
// https://github.com/eisop/checker-framework/issues/641

import org.checkerframework.checker.nullness.qual.Nullable;

final class FinalClassLambda1 {
    @Nullable String s;

    FinalClassLambda1() {
        use(this::init);
    }

    void init() {}

    static void use(Runnable r) {}
}

final class FinalClassLambda2 extends FinalClassLambda2Base {
    @Nullable String s;

    FinalClassLambda2() {
        use(() -> init());
        use(
                new Runnable() {
                    @Override
                    public void run() {
                        init();
                    }
                });
    }

    void init() {}
}

class FinalClassLambda2Base {
    void use(Runnable r) {}
}

final class FinalClassLambda3 {
    String s;

    FinalClassLambda3() {
        s = "hello";
        use(this::init);
    }

    void init() {}

    static void use(Runnable r) {}
}

final class FinalClassLambda4 extends FinalClassLambda2Base {
    String s;

    FinalClassLambda4() {
        s = "world";
        use(() -> init());
        use(
                new Runnable() {
                    @Override
                    public void run() {
                        init();
                    }
                });
    }

    void init() {}
}

// Not a final class, but uses same name for consistency.
class FinalClassLambda5 extends FinalClassLambda2Base {
    String s;

    FinalClassLambda5() {
        s = "hello";
        // :: error: (method.invocation.invalid)
        use(
                // :: error: (methodref.receiver.bound.invalid)
                this::init);
    }

    FinalClassLambda5(int dummy) {
        s = "world";
        // :: error: (method.invocation.invalid)
        use(
                // :: error: (method.invocation.invalid)
                () -> init());
        // :: error: (method.invocation.invalid)
        use(
                new Runnable() {
                    @Override
                    public void run() {
                        // :: error: (method.invocation.invalid)
                        init();
                    }
                });
    }

    void init() {}
}
