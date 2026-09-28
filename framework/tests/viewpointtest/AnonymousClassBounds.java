import org.checkerframework.framework.qual.DefaultQualifierForUse;

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

    @DefaultQualifierForUse(A.class)
    interface UseDefIface {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @DefaultQualifierForUse(A.class)
    static class UseDefClass {}

    void testUnannotated() {
        // Unannotated anonymous class creation defaults to the declaration bound of the
        // supertype (@A), rather than @Top.
        @A AClass a1 = new AClass() {};
        @Top AClass a2 = new AClass() {};
        // :: error: (assignment.type.incompatible)
        @B AClass a3 = new AClass() {};

        // Unannotated interface implementation
        @A AIface i1 = new AIface() {};
        @Top AIface i2 = new AIface() {};
        // :: error: (assignment.type.incompatible)
        @B AIface i3 = new AIface() {};

        // Unannotated parameterized class
        @A GClass<String> g1 = new GClass<String>() {};
        // :: error: (assignment.type.incompatible)
        @B GClass<String> g2 = new GClass<String>() {};

        // Unannotated bounded parameterized class
        @A GBoundedClass<@A String> gb1 = new GBoundedClass<@A String>() {};
        // :: error: (assignment.type.incompatible)
        @B GBoundedClass<@A String> gb2 = new GBoundedClass<@A String>() {};

        // Unannotated nested anonymous class
        new AClass() {
            void m() {
                @A AClass nested = new AClass() {};
                // :: error: (assignment.type.incompatible)
                @B AClass nestedBad = new AClass() {};
            }
        };

        // Unannotated anonymous class with @DefaultQualifierForUse on interface
        @A UseDefIface u1 = new UseDefIface() {};
        @Top UseDefIface u2 = new UseDefIface() {};
        // :: error: (assignment.type.incompatible)
        @B UseDefIface u3 = new UseDefIface() {};

        // Unannotated anonymous class with @DefaultQualifierForUse on class
        @A UseDefClass uc1 = new UseDefClass() {};
        @Top UseDefClass uc2 = new UseDefClass() {};
        // :: error: (assignment.type.incompatible)
        @B UseDefClass uc3 = new UseDefClass() {};
    }

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
        // @A is AIface's declaration bound, so this use is valid.
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

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @A static class GBoundedClass<T extends @A Object> {}

    void testTypeArgumentBounds() {
        new @A GBoundedClass<@A String>() {};

        // :: error: (type.argument.type.incompatible)
        new @A GBoundedClass<@B String>() {};

        // :: error: (type.argument.type.incompatible)
        new @A GBoundedClass<@Top String>() {};
    }

    void testNested() {
        new @A AClass() {
            void m() {
                new @A AClass() {};
                // :: warning: (cast.unsafe.constructor.invocation)
                // :: error: (type.invalid.annotations.on.use)
                new @B AClass() {};
            }
        };
    }
}
