package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultRestrictedBottom;

/**
 * Tests that @DefaultQualifier specifying a location prohibited by the qualifier's @TargetLocations
 * meta-annotation reports a compiler error.
 */
@DefaultQualifier(value = ElementDefaultRestrictedBottom.class, locations = TypeUseLocation.RETURN)
// :: error: (default.qualifier.prohibited.location)
public class ProhibitedLocationDefault {

    @DefaultQualifier(
            value = ElementDefaultRestrictedBottom.class,
            locations = TypeUseLocation.FIELD)
    // :: error: (default.qualifier.prohibited.location)
    Object field;

    @DefaultQualifier.List({
        @DefaultQualifier(
                value = ElementDefaultRestrictedBottom.class,
                locations = TypeUseLocation.LOCAL_VARIABLE)
    })
    // :: error: (default.qualifier.prohibited.location)
    void testList() {}

    // PARAMETER is permitted by @TargetLocations({PARAMETER, EXPLICIT_LOWER_BOUND}), so no error
    // here
    @DefaultQualifier(
            value = ElementDefaultRestrictedBottom.class,
            locations = TypeUseLocation.PARAMETER)
    void testParam(Object x) {}
}
