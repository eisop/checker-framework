// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
import viewpointtest.quals.*;

public class SuperConstructorCalls {

    public SuperConstructorCalls() {}

    public SuperConstructorCalls(@ReceiverDependentQual Object obj) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @ReceiverDependentQual SuperConstructorCalls(
            @ReceiverDependentQual Object obj, int dummy) {}

    class Inner extends SuperConstructorCalls {
        public Inner() {
            super();
        }

        // The constructor's return type is implicitly @Top by default.
        // When calling the super constructor, @Top becomes @Lost in the super constructor's
        // signature, causing a type mismatch with the expected @ReceiverDependentQual parameter.
        public Inner(@Top Object objTop) {
            // :: error: (argument.type.incompatible)
            super(objTop);
        }

        @SuppressWarnings("inconsistent.constructor.type")
        public @A Inner(@A Object objA, int dummy) {
            super(objA, 0);
        }

        @SuppressWarnings("inconsistent.constructor.type")
        public @A Inner(@A Object objA, @B Object objB) {
            // :: error: (super.invocation.invalid)
            super(objA);
        }

        @SuppressWarnings("inconsistent.constructor.type")
        public @A Inner(@A Object objA, @B Object objB, int dummy) {
            // :: error: (argument.type.incompatible)
            super(objB, 0);
        }
    }
}
