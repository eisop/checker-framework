import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

// KeyForBottom is only permitted at LOWER_BOUND, UPPER_BOUND, and PARAMETER.
// Setting it as default for RETURN, FIELD, or LOCAL_VARIABLE is prohibited.
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

    // PARAMETER is permitted: no error
    @DefaultQualifier(value = KeyForBottom.class, locations = TypeUseLocation.PARAMETER)
    void testParam(Object x) {}
}
