// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
import viewpointtest.quals.*;

public class SuperConstructorCalls extends superClass {
    public SuperConstructorCalls() {}

    public SuperConstructorCalls(Object obj) {
        super();
    }

    public SuperConstructorCalls(int number, @Top Object obj) {
        super(obj);
    }

    public SuperConstructorCalls(int number, @A Object obj1, @B Object obj2) {
        super(obj1);
    }

    public SuperConstructorCalls(int number, @B Object obj1, @A Object obj2, @Top Object obj3) {
        super(obj1);
    }

    public SuperConstructorCalls(@Top float number, @Top Object obj) {
        // :: error: (argument.type.incompatible)
        super(number);
    }

    public SuperConstructorCalls(@A float number, @A Object obj1, @B Object obj2) {
        super(number);
    }

    public SuperConstructorCalls(
            @B float number, @Top Object obj1, @A Object obj2, @B Object obj3) {
        // :: error: (argument.type.incompatible)
        super(number);
    }
}

class superClass {
    public superClass() {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @ReceiverDependentQual superClass(@ReceiverDependentQual Object str) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @A superClass(@A float number) {}
}
