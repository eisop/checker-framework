import viewpointtest.quals.*;

// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
public class ThisConstructorCalls {
    public ThisConstructorCalls() {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @ReceiverDependentQual ThisConstructorCalls(@ReceiverDependentQual Object str) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @A ThisConstructorCalls(@A float number) {}

    public ThisConstructorCalls(String str) {
        this();
    }

    public ThisConstructorCalls(String str, @A Object obj) {
        this(obj);
    }

    // :: warning: (inconsistent.constructor.type)
    public @A ThisConstructorCalls(Boolean bool, @B Object obj) {
        // :: error: (argument.type.incompatible)
        this(obj);
    }

    public ThisConstructorCalls(int integer, @Top Object obj) {
        this(obj);
    }

    public ThisConstructorCalls(@A float number, Object obj) {
        this(number);
    }

    public ThisConstructorCalls(@B float number, Object obj1, Object obj2) {
        // :: error: (argument.type.incompatible)
        this(number);
    }
}
