// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
import viewpointtest.quals.*;

@ReceiverDependentQual public class SuperConstructorCalls {
    public SuperConstructorCalls() {}

    public SuperConstructorCalls(@ReceiverDependentQual Object obj) {}

    public @ReceiverDependentQual SuperConstructorCalls(
            @ReceiverDependentQual Object obj, int dummy) {}

    @ReceiverDependentQual class Inner extends SuperConstructorCalls {
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

        public @A Inner(@A Object objA, int dummy) {
            super(objA, 0);
        }

        public @A Inner(@A Object objA, @B Object objB) {
            super(objA);
        }

        public @A Inner(@A Object objA, @B Object objB, int dummy) {
            // :: error: (argument.type.incompatible)
            super(objB, 0);
        }
    }
}
