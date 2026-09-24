import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

// KeyForBottom is only permitted at LOWER_BOUND and UPPER_BOUND in source code.
// Setting it as default in source for RETURN, FIELD, LOCAL_VARIABLE, or PARAMETER is prohibited.
public class DefaultQualifierProhibitedLocation {

    // :: error: (default.qualifier.prohibited.location)
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.RETURN)
    void testReturn() {}

    // :: error: (default.qualifier.prohibited.location)
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.FIELD)
    static class TestField {
        Object f = new Object();
    }

    @DefaultQualifier.List({
        // :: error: (default.qualifier.prohibited.location)
        @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.LOCAL_VARIABLE)
    })
    void testList() {}

    // PARAMETER is prohibited for written @DefaultQualifier (even with
    // @ProgrammaticDefaultLocations)
    // :: error: (default.qualifier.prohibited.location)
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.PARAMETER)
    void testParam(Object x) {}

    // LOWER_BOUND is permitted in source: no error
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.LOWER_BOUND)
    void testLowerBound() {}
}
