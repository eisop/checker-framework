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
        new @A VarargsConstructor(aObj);
        new @B VarargsConstructor(bObj);
        new @Top VarargsConstructor(topObj);
        // :: error: (argument.type.incompatible)
        new @A VarargsConstructor(bObj);
        // :: error: (argument.type.incompatible)
        new @B VarargsConstructor(aObj);
    }

    // inner class
    class Inner {
        Inner(Object... args) {}

        void foo() {
            Inner a = new Inner();
            Inner b = new Inner(new Object());
            Inner c = VarargsConstructor.this.new Inner();
            Inner d = VarargsConstructor.this.new Inner(new Object());
        }
    }

    // anonymous class
    Object o =
            new VarargType("testStr", new Object()) {
                void foo() {
                    VarargsConstructor a = new VarargsConstructor("testStr", new Object());
                }
            };
}
