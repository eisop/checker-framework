import viewpointtest.quals.*;

// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
public class ThisConstructorCalls {
    public ThisConstructorCalls() {}

    public ThisConstructorCalls(@ReceiverDependentQual Object obj) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @ReceiverDependentQual ThisConstructorCalls(
            @ReceiverDependentQual Object obj, int dummy) {}

    @SuppressWarnings("inconsistent.constructor.type")
    public @A ThisConstructorCalls(@A Object objA, int dummy1, int dummy2) {
        this(objA, 0);
    }

    @SuppressWarnings("inconsistent.constructor.type")
    public @A ThisConstructorCalls(@B Object objB, int dummy, int dummy2, int dummy3) {
        // :: error: (argument.type.incompatible)
        this(objB, 0);
    }

    @SuppressWarnings("inconsistent.constructor.type")
    public @A ThisConstructorCalls(@A Object objA, @B Object objB) {
        // :: error: (this.invocation.invalid)
        this(objA);
    }
}
