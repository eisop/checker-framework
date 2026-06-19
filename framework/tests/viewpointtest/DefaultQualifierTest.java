import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import viewpointtest.quals.*;

@DefaultQualifier(
        value = A.class,
        locations = {TypeUseLocation.TYPE})
public class DefaultQualifierTest {
    // :: error: (super.invocation.invalid)
    // :: warning: (inconsistent.constructor.type)
    @B DefaultQualifierTest() {}
}

/**
 * Explicit {@link A} on the class matches the TYPE default; {@link B} on the constructor is
 * invalid.
 */
@DefaultQualifier(
        value = A.class,
        locations = {TypeUseLocation.TYPE})
@A class DefaultQualifierExplicitAOnClass {
    // :: error: (type.invalid.annotations.on.use)
    // :: error: (super.invocation.invalid)
    // :: warning: (inconsistent.constructor.type)
    @B DefaultQualifierExplicitAOnClass() {}
}

/** Explicit {@link B} on the class overrides the TYPE default of {@link A}. */
@DefaultQualifier(
        value = A.class,
        locations = {TypeUseLocation.TYPE})
@B @SuppressWarnings("inconsistent.constructor.type")
class DefaultQualifierExplicitBOnClass {
    // :: error: (super.invocation.invalid)
    DefaultQualifierExplicitBOnClass() {}
}

/** The TYPE default also applies to nested class declarations. */
@DefaultQualifier(
        value = A.class,
        locations = {TypeUseLocation.TYPE})
// :: error: (super.invocation.invalid)
// :: warning: (inconsistent.constructor.type)
@A class DefaultQualifierNestedClasses {
    class DefaultedNested {
        // :: error: (super.invocation.invalid)
        // :: warning: (inconsistent.constructor.type)
        @B DefaultedNested() {}
    }
}
