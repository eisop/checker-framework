// Test that an override check resolves method signatures for the receiver that the overriding
// method declares. A @ReceiverDependentQual type in the supertype is a template that resolves to
// the overriding method's receiver qualifier.

import viewpointtest.quals.*;

public class OverrideViewpointAdaptation {

    interface Accessor {
        // Return type test methods
        @ReceiverDependentQual Object getReturnExact(@ReceiverDependentQual Accessor this);

        @ReceiverDependentQual Object getReturnNarrowedBottom(@ReceiverDependentQual Accessor this);

        @ReceiverDependentQual Object getReturnWidenedTop(@ReceiverDependentQual Accessor this);

        @ReceiverDependentQual Object getReturnOther(@ReceiverDependentQual Accessor this);

        // Parameter type test methods
        void setParamExact(@ReceiverDependentQual Accessor this, @ReceiverDependentQual Object o);

        void setParamWidenedTop(
                @ReceiverDependentQual Accessor this, @ReceiverDependentQual Object o);

        void setParamNarrowedBottom(
                @ReceiverDependentQual Accessor this, @ReceiverDependentQual Object o);

        void setParamOther(@ReceiverDependentQual Accessor this, @ReceiverDependentQual Object o);
    }

    // =========================================================================
    // 1. @A Interface Testing All Method Overrides
    // =========================================================================

    @A interface SubA extends Accessor {

        // --- Return Type Checks ---

        @Override
        @A Object getReturnExact(@A SubA this);

        @Override
        @Bottom Object getReturnNarrowedBottom(@A SubA this);

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop(@A SubA this);

        @Override
        // :: error: (override.return.invalid)
        @B Object getReturnOther(@A SubA this);

        // --- Parameter Type Checks ---

        @Override
        void setParamExact(@A SubA this, @A Object o);

        @Override
        void setParamWidenedTop(@A SubA this, @Top Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamNarrowedBottom(@A SubA this, @Bottom Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamOther(@A SubA this, @B Object o);
    }

    // =========================================================================
    // 2. @B Interface Testing Symmetrical Method Overrides
    // =========================================================================

    @B interface SubB extends Accessor {

        // --- Return Type Checks ---

        @Override
        @B Object getReturnExact(@B SubB this);

        @Override
        @Bottom Object getReturnNarrowedBottom(@B SubB this);

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop(@B SubB this);

        @Override
        // :: error: (override.return.invalid)
        @A Object getReturnOther(@B SubB this);

        // --- Parameter Type Checks ---

        @Override
        void setParamExact(@B SubB this, @B Object o);

        @Override
        void setParamWidenedTop(@B SubB this, @Top Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamNarrowedBottom(@B SubB this, @Bottom Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamOther(@B SubB this, @A Object o);
    }

    // =========================================================================
    // 3. Receiver-Dependent Interface Testing Symmetrical Method Overrides
    // =========================================================================

    @ReceiverDependentQual interface SubReceiverDependent extends Accessor {

        // --- Return Type Checks ---

        @Override
        @ReceiverDependentQual Object getReturnExact(@ReceiverDependentQual SubReceiverDependent this);

        @Override
        @Bottom Object getReturnNarrowedBottom(@ReceiverDependentQual SubReceiverDependent this);

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop(@ReceiverDependentQual SubReceiverDependent this);

        @Override
        // :: error: (override.return.invalid)
        @A Object getReturnOther(@ReceiverDependentQual SubReceiverDependent this);

        // --- Parameter Type Checks ---

        @Override
        void setParamExact(
                @ReceiverDependentQual SubReceiverDependent this, @ReceiverDependentQual Object o);

        @Override
        void setParamWidenedTop(@ReceiverDependentQual SubReceiverDependent this, @Top Object o);

        @Override
        void setParamNarrowedBottom(
                // :: error: (override.param.invalid)
                @ReceiverDependentQual SubReceiverDependent this, @Bottom Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamOther(@ReceiverDependentQual SubReceiverDependent this, @A Object o);
    }

    // =========================================================================
    // 4. Implicit Receiver Parameter (default receiver type from class bound)
    // =========================================================================

    @A interface SubAImplicitReceiver extends Accessor {

        @Override
        @A Object getReturnExact();

        @Override
        @Bottom Object getReturnNarrowedBottom();

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop();

        @Override
        // :: error: (override.return.invalid)
        @B Object getReturnOther();

        @Override
        void setParamExact(@A Object o);

        @Override
        void setParamWidenedTop(@Top Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamNarrowedBottom(@Bottom Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamOther(@B Object o);
    }

    // =========================================================================
    // 5. Class Inheritance (SuperClass -> SubClass)
    // =========================================================================

    static class SuperClass {
        @ReceiverDependentQual Object getReturnExact(@ReceiverDependentQual SuperClass this) {
            return null;
        }

        @ReceiverDependentQual Object getReturnNarrowedBottom(@ReceiverDependentQual SuperClass this) {
            return null;
        }

        @ReceiverDependentQual Object getReturnWidenedTop(@ReceiverDependentQual SuperClass this) {
            return null;
        }

        @ReceiverDependentQual Object getReturnOther(@ReceiverDependentQual SuperClass this) {
            return null;
        }

        void setParamExact(
                @ReceiverDependentQual SuperClass this, @ReceiverDependentQual Object o) {}

        void setParamWidenedTop(
                @ReceiverDependentQual SuperClass this, @ReceiverDependentQual Object o) {}

        void setParamNarrowedBottom(
                @ReceiverDependentQual SuperClass this, @ReceiverDependentQual Object o) {}

        void setParamOther(
                @ReceiverDependentQual SuperClass this, @ReceiverDependentQual Object o) {}
    }

    @A static class SubClassA extends SuperClass {

        @Override
        @A Object getReturnExact() {
            return null;
        }

        @Override
        @Bottom Object getReturnNarrowedBottom() {
            return null;
        }

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop() {
            return null;
        }

        @Override
        // :: error: (override.return.invalid)
        @B Object getReturnOther() {
            return null;
        }

        @Override
        void setParamExact(@A Object o) {}

        @Override
        void setParamWidenedTop(@Top Object o) {}

        @Override
        // :: error: (override.param.invalid)
        void setParamNarrowedBottom(@Bottom Object o) {}

        @Override
        // :: error: (override.param.invalid)
        void setParamOther(@B Object o) {}
    }

    // =========================================================================
    // 6. Transitive / Multi-Level Inheritance
    // =========================================================================

    @ReceiverDependentQual interface MiddleRD extends Accessor {}

    @A interface LeafA extends MiddleRD {

        @Override
        @A Object getReturnExact();

        @Override
        // :: error: (override.return.invalid)
        @Top Object getReturnWidenedTop();

        @Override
        void setParamExact(@A Object o);

        @Override
        // :: error: (override.param.invalid)
        void setParamNarrowedBottom(@Bottom Object o);
    }

    // =========================================================================
    // 7. Non-Receiver-Dependent Fixed Qualifier in Supertype
    // =========================================================================

    interface FixedAccessor {
        @A Object getFixedAExact();

        @A Object getFixedAOther();

        void setFixedAExact(@A Object o);

        void setFixedANarrowed(@A Object o);
    }

    @B interface SubBFixed extends FixedAccessor {

        @Override
        @A Object getFixedAExact();

        @Override
        // :: error: (override.return.invalid)
        @B Object getFixedAOther();

        @Override
        void setFixedAExact(@A Object o);

        @Override
        // :: error: (override.param.invalid)
        void setFixedANarrowed(@Bottom Object o);
    }
}
