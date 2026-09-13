package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultTop;

/**
 * Two repeated {@code @DefaultQualifier} annotations set RETURN in the same qualifier hierarchy to
 * different qualifiers. Only one qualifier from a hierarchy can be the default for a location, so
 * this is reported rather than silently resolved by annotation ordering. The first one written
 * takes effect, so RETURN is Bottom here.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
@DefaultQualifier(value = ElementDefaultTop.class, locations = TypeUseLocation.RETURN)
// :: error: (conflicting.defaults)
public class ConflictingWrittenDefaults {
    Object getBottom() {
        // :: error: (return.type.incompatible)
        return new Object();
    }

    @DefaultQualifier.List({
        @DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN),
        @DefaultQualifier(value = ElementDefaultTop.class, locations = TypeUseLocation.RETURN)
    })
    // :: error: (conflicting.defaults)
    static class ConflictingWrittenDefaultsWithList {
        Object getBottom() {
            // :: error: (return.type.incompatible)
            return new Object();
        }
    }

    @DefaultQualifier.List({
        @DefaultQualifier(value = ElementDefaultTop.class, locations = TypeUseLocation.RETURN),
        @DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
    })
    // :: error: (conflicting.defaults)
    static class ConflictingWrittenDefaultsWithListTopFirst {
        Object getTop() {
            return new Object();
        }
    }
}
