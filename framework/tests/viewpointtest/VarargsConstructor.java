// Test case for EISOP issue #777:
// https://github.com/eisop/checker-framework/issues/777
import viewpointtest.quals.*;

public class VarargsConstructor {

    VarargsConstructor(String str, Object... args) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual
    VarargsConstructor(@ReceiverDependentQual Object... args) {}

    void foo() {
        VarargsConstructor a = new VarargsConstructor("testStr", new Object());
    }

    void invokeConstructor(@A Object aObj, @B Object bObj, @Top Object topObj) {
        @A Object a = new @A VarargsConstructor(aObj);
        @B Object b = new @B VarargsConstructor(bObj);
        @Top Object top = new @Top VarargsConstructor(topObj);
        // :: error: (argument.type.incompatible)
        new @A VarargsConstructor(bObj);
        // :: error: (argument.type.incompatible)
        new @B VarargsConstructor(aObj);
    }

    class Inner {
        @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
        @ReceiverDependentQual
        Inner(@ReceiverDependentQual Object... args) {}

        void foo() {
            Inner a = new Inner();
            Inner b = new Inner(new Object());
            Inner c = VarargsConstructor.this.new Inner();
            Inner d = VarargsConstructor.this.new Inner(new Object());
        }

        void invokeConstructor(@A Object aObj, @B Object bObj, @Top Object topObj) {
            @A Object a = new @A Inner(aObj);
            @B Object b = new @B Inner(bObj);
            @Top Object top = new @Top Inner(topObj);
            // :: error: (argument.type.incompatible)
            new @A Inner(bObj);
            // :: error: (argument.type.incompatible)
            new @B Inner(aObj);
        }
    }

    void testAnonymousClass(@A Object aObj, @B Object bObj, @Top Object topObj) {
        Object o =
                new VarargsConstructor("testStr", new Object()) {
                    void foo() {
                        VarargsConstructor a = new VarargsConstructor("testStr", new Object());
                    }
                };
        @A Object a = new @A VarargsConstructor(aObj) {};
        @B Object b = new @B VarargsConstructor(bObj) {};
        @Top Object top = new @Top VarargsConstructor(topObj) {};
        // :: error: (argument.type.incompatible)
        new @A VarargsConstructor(bObj) {};
        // :: error: (argument.type.incompatible)
        new @B VarargsConstructor(aObj) {};
    }
}
