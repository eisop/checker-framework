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
}
