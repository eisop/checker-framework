package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultTop;

/**
 * Tests that {@code addElementDefault} on this class, called after defaults for this class and for
 * its nested class have already been queried and memoized, still applies everywhere the memoized
 * defaults do.
 *
 * <p>This class and {@link OrderBeforeClass} are identical apart from their names, and must produce
 * identical diagnostics: that identity is the invariant under test. See eisop#2047.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.LOCAL_VARIABLE)
public class OrderAfterClass {
    // Inherited from addElementDefault on package elementdefault.pkg: FIELD is Bottom
    Object f;

    // Specified by written @DefaultQualifier: RETURN is Bottom
    Object getBottom() {
        // :: error: (return.type.incompatible)
        return new Object();
    }

    // Specified by a repeated @DefaultQualifier annotation: LOCAL_VARIABLE is Bottom
    void testLocal() {
        // :: error: (assignment.type.incompatible)
        Object local = new Object();
    }

    // Specified by addElementDefault on this class: PARAMETER is Bottom
    void takeBottom(Object param) {}

    /**
     * A member with a written {@code @DefaultQualifier} of its own, so that QualifierDefaults
     * memoizes a default set for it that is a distinct object from the enclosing class's. The
     * enclosing class's programmatic PARAMETER default must still reach it.
     */
    @DefaultQualifier(value = ElementDefaultTop.class, locations = TypeUseLocation.RETURN)
    static class Nested {
        // This class's own written @DefaultQualifier shadows the enclosing class's RETURN
        // default, so returning an unqualified (Top) value is fine here.
        Object getTop() {
            return new Object();
        }

        // Inherited from addElementDefault on the enclosing class: PARAMETER is Bottom
        void takeBottomNested(Object param) {}
    }

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
        // :: error: (argument.type.incompatible)
        takeBottom(new Object());
        // :: error: (argument.type.incompatible)
        new Nested().takeBottomNested(new Object());
    }
}
