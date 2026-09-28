// Test case for EISOP issue #386:
// https://github.com/eisop/checker-framework/issues/386
import viewpointtest.quals.*;

public class Issue386 {
    public class Inner {
        Inner() {}

        Inner(@ReceiverDependentQual Object... args) {}
    }

    public class MethodReceiver {
        void method(@ReceiverDependentQual Object... args) {}
    }

    @SuppressWarnings("cast.unsafe.constructor.invocation")
    public void constructorTest(@A Object aObj, @A Object otherAObj, @B Object bObj) {
        this.new @A Inner(aObj, otherAObj);
        // :: error: (argument.type.incompatible)
        this.new @A Inner(aObj, bObj);
    }

    public void methodTest(
            @A MethodReceiver receiver, @A Object aObj, @A Object otherAObj, @B Object bObj) {
        receiver.method(aObj, otherAObj);
        // :: error: (argument.type.incompatible)
        receiver.method(aObj, bObj);
    }
}
