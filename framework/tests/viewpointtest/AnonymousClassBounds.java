import viewpointtest.quals.*;

/**
 * Tests that a type-use annotation written on an anonymous class creation expression is validated
 * against the declaration bound of the class being extended.
 *
 * <p>In Java 11 and lower, javac attaches that annotation to the anonymous class declaration's
 * modifiers rather than to its extends clause, so this check must not depend on the extends clause
 * carrying the annotation.
 */
public class AnonymousClassBounds {
    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @A static class AClass {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @A interface AIface {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @A static class GClass<T> {}

    void test() {
        // @A is AClass's declaration bound, so this use is valid.
        new @A AClass() {};

        // @B is a sibling of @A, so it is outside AClass's declaration bound.
        // :: warning: (cast.unsafe.constructor.invocation)
        // :: error: (type.invalid.annotations.on.use)
        new @B AClass() {};

        // @Bottom is below the declaration bound, so this use is valid.
        // :: warning: (cast.unsafe.constructor.invocation)
        new @Bottom AClass() {};

        // @Top is above the declaration bound.
        // :: error: (new.class.type.invalid)
        // :: error: (type.invalid.annotations.on.use)
        new @Top AClass() {};
    }

    // The same cases, written with a qualified type, whose extends clause is a MEMBER_SELECT
    // rather than an IDENTIFIER.
    void testQualified() {
        new AnonymousClassBounds.@A AClass() {};

        // :: warning: (cast.unsafe.constructor.invocation)
        // :: error: (type.invalid.annotations.on.use)
        new AnonymousClassBounds.@B AClass() {};

        // :: error: (new.class.type.invalid)
        // :: error: (type.invalid.annotations.on.use)
        new AnonymousClassBounds.@Top AClass() {};
    }

    // The same cases for an anonymous class implementing an interface rather than extending a
    // class. On Java 11 and lower, this is the implements clause rather than the extends
    // clause, but javac's handling of it has the same version-dependent shape, so both halves
    // of the fix (TreeUtils.isTypeTree recognizing the clause as a type at all, and
    // AnnotatedTypeFactory finding the annotation on the anonymous class body's modifiers) are
    // needed together here too.
    void testInterface() {
        // @A is AIface's declaration bound, so this use is valid. (An anonymous class
        // implementing an interface has no declared constructor to check consistency against,
        // so this warning is issued regardless of whether the annotation matches the bound.)
        // :: warning: (cast.unsafe.constructor.invocation)
        new @A AIface() {};

        // @B is a sibling of @A, so it is outside AIface's declaration bound.
        // :: warning: (cast.unsafe.constructor.invocation)
        // :: error: (type.invalid.annotations.on.use)
        new @B AIface() {};

        // @Bottom is below the declaration bound, so this use is valid.
        // :: warning: (cast.unsafe.constructor.invocation)
        new @Bottom AIface() {};

        // @Top is above the declaration bound.
        // :: error: (new.class.type.invalid)
        // :: error: (type.invalid.annotations.on.use)
        new @Top AIface() {};
    }

    // The same cases for a parameterized type. Its extends clause is a PARAMETERIZED_TYPE,
    // already one of TreeUtils.typeTreeKinds() on every JDK, so this isolates the
    // AnnotatedTypeFactory half of the fix from the TreeUtils.isTypeTree half: the clause is
    // always recognized as a type, but the annotation still needs to be found on the anonymous
    // class body's modifiers on Java 11 and lower.
    void testParameterized() {
        // @A is GClass's declaration bound, so this use is valid.
        new @A GClass<String>() {};

        // @B is a sibling of @A, so it is outside GClass's declaration bound.
        // :: warning: (cast.unsafe.constructor.invocation)
        // :: error: (type.invalid.annotations.on.use)
        new @B GClass<String>() {};

        // @Bottom is below the declaration bound, so this use is valid.
        // :: warning: (cast.unsafe.constructor.invocation)
        new @Bottom GClass<String>() {};

        // @Top is above the declaration bound.
        // :: error: (new.class.type.invalid)
        // :: error: (type.invalid.annotations.on.use)
        new @Top GClass<String>() {};
    }
}
