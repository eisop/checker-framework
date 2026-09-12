// @NullMarked aliases to @DefaultQualifier(NonNull.class, locations = UPPER_BOUND), so it is the
// one mechanism that can put a synthesized @DefaultQualifier on an element that also carries a
// written @DefaultQualifier.List (what javac produces for a repeated @DefaultQualifier).  These
// two classes pin what that combination does.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.jspecify.annotations.NullMarked;

/**
 * Repeated @DefaultQualifier annotations that do not touch UPPER_BOUND compose with @NullMarked.
 */
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.RETURN)
public class RepeatedDefaultQualifier<T> {
    Object f = null;

    Object get() {
        return null;
    }
}

/**
 * One of the repeated @DefaultQualifier annotations sets UPPER_BOUND, which is exactly what
 * {@code @NullMarked} sets, to a different qualifier. Only one qualifier from a hierarchy can be
 * the default for a location, so this is an error rather than a silent pick.
 */
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
// :: error: (conflicting.defaults)
class RepeatedDefaultQualifierConflictingWithNullMarked<T> {
    Object f = null;
}
