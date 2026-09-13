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

    /**
     * The same conflict as the enclosing class, written as an explicit
     * {@code @DefaultQualifier.List} rather than as a repeated {@code @DefaultQualifier}. javac
     * collapses the repeated form into exactly this container, so by the time {@code
     * AnnotatedTypeFactory#getDefaultQualifierAnnotations} sees either one they are
     * indistinguishable: this case cannot fail unless the enclosing class's does. It is here to
     * document that the surface syntax a user writes is handled, not to cover a separate path. The
     * case below, which reverses the order, is the one that pins which of the two wins.
     */
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

    /**
     * The same two defaults in the other order, so Top is first and wins. Returning an unqualified
     * (Top) value is therefore legal here, whereas it is an error in the two cases above: that
     * difference is what shows the winner is decided by source order rather than by which qualifier
     * happens to sort first.
     */
    @DefaultQualifier.List({
        @DefaultQualifier(value = ElementDefaultTop.class, locations = TypeUseLocation.RETURN),
        @DefaultQualifier(value = ElementDefaultBottom.class, locations = TypeUseLocation.RETURN)
    })
    // :: error: (conflicting.defaults)
    static class ConflictingWrittenDefaultsWithListTopFirst {
        // RETURN is Top, from the first @DefaultQualifier in the list; no error here.
        Object getTop() {
            return new Object();
        }
    }
}
