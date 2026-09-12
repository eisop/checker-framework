package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;

/**
 * Tests that calling {@code addElementDefault} on this class BEFORE {@code defaultsAt} runs
 * correctly preserves this class's written {@code @DefaultQualifier} (RETURN is Bottom) and
 * inherits the enclosing package default (FIELD is Bottom), while applying the programmatic default
 * (PARAMETER is Bottom). See eisop#2047.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.LOCAL_VARIABLE)
public class OrderBeforeClass {
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

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
        // :: error: (argument.type.incompatible)
        takeBottom(new Object());
    }
}
