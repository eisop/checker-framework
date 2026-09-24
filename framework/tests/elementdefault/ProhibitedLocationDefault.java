package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultRestrictedBottom;

/**
 * Tests that @DefaultQualifier specifying a location prohibited by the qualifier's @TargetLocations
 * meta-annotation reports a compiler error.
 */
// :: error: (default.qualifier.prohibited.location)
@DefaultQualifier(value = ElementDefaultRestrictedBottom.class, locations = TypeUseLocation.RETURN)
public class ProhibitedLocationDefault {

    // :: error: (default.qualifier.prohibited.location)
    @DefaultQualifier(
            value = ElementDefaultRestrictedBottom.class,
            locations = TypeUseLocation.FIELD)
    Object field;

    @DefaultQualifier.List({
        // :: error: (default.qualifier.prohibited.location)
        @DefaultQualifier(
                value = ElementDefaultRestrictedBottom.class,
                locations = TypeUseLocation.LOCAL_VARIABLE)
    })
    void testList() {}

    // PARAMETER is permitted by @TargetLocations({PARAMETER, EXPLICIT_LOWER_BOUND}), so no error
    // here
    @DefaultQualifier(
            value = ElementDefaultRestrictedBottom.class,
            locations = TypeUseLocation.PARAMETER)
    void testParam(Object x) {}
}
