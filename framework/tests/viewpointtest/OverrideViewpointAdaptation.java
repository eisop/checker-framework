// Test that an override check resolves method signatures for the receiver that the overriding
// method declares. A @ReceiverDependentQual type in the supertype is a template that resolves to
// the overriding method's receiver qualifier.

import viewpointtest.quals.*;

public class OverrideViewpointAdaptation {

    // spotless:off
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
    // spotless:on
}
