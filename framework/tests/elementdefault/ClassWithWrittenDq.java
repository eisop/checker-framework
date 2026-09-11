package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;

/**
 * Tests that a class in a package with programmatic {@code addElementDefault} (defaulting FIELD to
 * Bottom) respects both its own written {@code @DefaultQualifier} (defaulting RETURN to Bottom) and
 * the inherited package default.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
public class ClassWithWrittenDq {
    // Inherited from addElementDefault on package elementdefault.pkg: FIELD is Bottom
    Object f;

    // Specified by written @DefaultQualifier: RETURN is Bottom
    Object getBottom() {
        // :: error: (return.type.incompatible)
        return new Object();
    }

    void use() {
        // :: error: (assignment.type.incompatible)
        f = new Object();
    }
}
