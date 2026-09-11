package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;

/**
 * Tests that calling {@code addElementDefault} on this class AFTER {@code defaultsAt} has already
 * queried and memoized defaults for this class invalidates the cache and produces identical results
 * to {@link OrderBeforeClass}. See eisop#2047.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
public class OrderAfterClass {
    // Inherited from addElementDefault on package elementdefault.pkg: FIELD is Bottom
    Object f;

    // Specified by written @DefaultQualifier: RETURN is Bottom
    Object getBottom() {
        // :: error: (return.type.incompatible)
        return new Object();
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
