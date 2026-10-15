package elementdefault.pkg;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultBottom;

/**
 * Two repeated {@code @DefaultQualifier} annotations that set RETURN to the same qualifier are
 * redundant, not conflicting, and must stay legal: no {@code conflicting.defaults} error here.
 */
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
@DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
public class RedundantWrittenDefaults {
    Object getBottom() {
        // :: error: (return.type.incompatible)
        return new Object();
    }
}
