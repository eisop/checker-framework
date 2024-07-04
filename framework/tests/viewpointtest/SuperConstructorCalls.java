// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
import viewpointtest.quals.*;

public class SuperConstructorCalls {

    public SuperConstructorCalls() {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @ReceiverDependentQual SuperConstructorCalls(@ReceiverDependentQual Object str) {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    public @A SuperConstructorCalls(@A float number) {}

    class Inner extends SuperConstructorCalls {
        public Inner() {}

        public Inner(Object obj) {
            super();
        }

        public Inner(int number, @Top Object obj) {
            super(obj);
        }

        // :: warning: (inconsistent.constructor.type)
        public @A Inner(int number, @A Object obj1, @B Object obj2) {
            super(obj1);
        }

        // :: warning: (inconsistent.constructor.type)
        public @B Inner(int number, @B Object obj1, @A Object obj2, @Top Object obj3) {
            super(obj1);
        }

        public Inner(@Top float number, @Top Object obj) {
            // :: error: (argument.type.incompatible)
            super(number);
        }

        // :: warning: (inconsistent.constructor.type)
        public @A Inner(@A float number, @A Object obj1, @B Object obj2) {
            super(number);
        }

        public Inner(@B float number, @Top Object obj1, @A Object obj2, @B Object obj3) {
            // :: error: (argument.type.incompatible)
            super(number);
        }
    }
}
