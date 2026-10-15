import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

// KeyForBottom is only permitted at LOWER_BOUND and UPPER_BOUND in source code.
// Setting it as default in source for RETURN, FIELD, LOCAL_VARIABLE, or PARAMETER is prohibited.
public class DefaultQualifierProhibitedLocation {

    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.RETURN)
    // :: error: (default.qualifier.prohibited.location)
    void testReturn() {}

    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.FIELD)
    // :: error: (default.qualifier.prohibited.location)
    static class TestField {
        Object f = new Object();
    }

    @DefaultQualifier.List({
        @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.LOCAL_VARIABLE)
    })
    // :: error: (default.qualifier.prohibited.location)
    void testList() {}

    // PARAMETER is prohibited for written @DefaultQualifier (even with
    // @ProgrammaticDefaultLocations)
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.PARAMETER)
    // :: error: (default.qualifier.prohibited.location)
    void testParam(Object x) {}

    // LOWER_BOUND is permitted in source: no error
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.LOWER_BOUND)
    void testLowerBound() {}
}
