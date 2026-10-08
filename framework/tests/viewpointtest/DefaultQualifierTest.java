import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import viewpointtest.quals.*;

public class DefaultQualifierTest {
    @DefaultQualifier(
            value = A.class,
            locations = {TypeUseLocation.TYPE})
    class DefaultAClass {
        // :: error: (type.invalid.annotations.on.use)
        // :: warning: (inconsistent.constructor.type)
        // :: error: (super.invocation.invalid)
        @B DefaultAClass() {}

        void typeUses() {
            @A DefaultAClass defaultAsA;
            @B DefaultAClass defaultAsB;
        }
    }

    @A class ExplicitAClass {
        // :: error: (type.invalid.annotations.on.use)
        // :: warning: (inconsistent.constructor.type)
        // :: error: (super.invocation.invalid)
        @B ExplicitAClass() {}

        void typeUses() {
            @A ExplicitAClass explicitAsA;
            @B ExplicitAClass explicitAsB;
        }
    }

    @DefaultQualifier(
            value = B.class,
            locations = {TypeUseLocation.TYPE})
    class DefaultBClass {
        // :: error: (type.invalid.annotations.on.use)
        // :: warning: (inconsistent.constructor.type)
        // :: error: (super.invocation.invalid)
        @A DefaultBClass() {}

        void typeUses() {
            @B DefaultBClass defaultAsB;
            @A DefaultBClass defaultAsA;
        }
    }

    @B class ExplicitBClass {
        // :: error: (type.invalid.annotations.on.use)
        // :: warning: (inconsistent.constructor.type)
        // :: error: (super.invocation.invalid)
        @A ExplicitBClass() {}

        void typeUses() {
            @B ExplicitBClass explicitAsB;
            @A ExplicitBClass explicitAsA;
        }
    }
}
