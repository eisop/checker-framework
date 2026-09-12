// @NullMarked aliases to @DefaultQualifier(NonNull.class, locations = UPPER_BOUND), so it is the
// one mechanism that can put a synthesized @DefaultQualifier on an element that also carries a
// written @DefaultQualifier (or a written @DefaultQualifier.List, which is what javac produces
// for a repeated @DefaultQualifier).  These classes pin what that combination does.
//
// Among the @DefaultQualifier annotations that apply to a declaration -- written or contributed
// by an alias -- the one appearing first in the source wins; a later conflicting one is reported
// as a conflicting.defaults error and discarded.  Each conflict below therefore appears twice, in
// both orders, and the two orders produce different defaults.
//
// The observable for UPPER_BOUND is a use of the class with a @Nullable type argument: it is an
// error when UPPER_BOUND defaults to @NonNull (what @NullMarked sets) and is accepted when
// UPPER_BOUND defaults to @Nullable (what the written @DefaultQualifier below sets).  Without
// either, a type parameter's upper bound defaults to @Nullable.

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

    // UPPER_BOUND is @NonNull, from @NullMarked.
    // :: error: (type.argument.type.incompatible)
    void use(RepeatedDefaultQualifier<@Nullable String> p) {}
}

/**
 * A single written @DefaultQualifier for a location that @NullMarked says nothing about. Both
 * apply: @DefaultQualifier is not allowed to hide the alias. See eisop issue #2064.
 */
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
class SingleDefaultQualifierWithNullMarked<T> {
    // The written @DefaultQualifier applies: fields default to @Nullable.
    Object f = null;

    // @NullMarked's UPPER_BOUND default applies: the type argument must be @NonNull.
    // :: error: (type.argument.type.incompatible)
    void use(SingleDefaultQualifierWithNullMarked<@Nullable String> p) {}
}

/**
 * {@code @NullMarked} is written first, so its UPPER_BOUND default wins over the written
 * {@code @DefaultQualifier} that follows it.
 */
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
// :: error: (conflicting.defaults)
class NullMarkedBeforeDefaultQualifier<T> {
    // UPPER_BOUND is @NonNull, from @NullMarked.
    // :: error: (type.argument.type.incompatible)
    void use(NullMarkedBeforeDefaultQualifier<@Nullable String> p) {}
}

/**
 * The same two annotations in the other order: the written {@code @DefaultQualifier} is first, so
 * it wins and {@code @NullMarked}'s UPPER_BOUND default is the one discarded.
 */
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
@NullMarked
// :: error: (conflicting.defaults)
class DefaultQualifierBeforeNullMarked<T> {
    // UPPER_BOUND is @Nullable, from the written @DefaultQualifier; no error here.
    void use(DefaultQualifierBeforeNullMarked<@Nullable String> p) {}
}

/**
 * One of the repeated @DefaultQualifier annotations sets UPPER_BOUND, which is exactly what
 * {@code @NullMarked} sets, to a different qualifier. Only one qualifier from a hierarchy can be
 * the default for a location, so this is an error rather than a silent pick. {@code @NullMarked} is
 * first, so it wins.
 */
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
// :: error: (conflicting.defaults)
class RepeatedDefaultQualifierConflictingWithNullMarked<T> {
    Object f = null;

    // UPPER_BOUND is @NonNull, from @NullMarked.
    // :: error: (type.argument.type.incompatible)
    void use(RepeatedDefaultQualifierConflictingWithNullMarked<@Nullable String> p) {}
}

/**
 * The same repeated @DefaultQualifier annotations, now written before {@code @NullMarked}: the
 * written UPPER_BOUND default wins.
 */
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@NullMarked
// :: error: (conflicting.defaults)
class RepeatedDefaultQualifierBeforeNullMarked<T> {
    Object f = null;

    // UPPER_BOUND is @Nullable, from the written @DefaultQualifier; no error here.
    void use(RepeatedDefaultQualifierBeforeNullMarked<@Nullable String> p) {}
}

/**
 * {@code @NullMarked} written between two {@code @DefaultQualifier} annotations. javac collapses
 * repeated annotations into a single {@code @DefaultQualifier.List} at the position of the first
 * one, so both written defaults precede {@code @NullMarked} even though the second is textually
 * after it, and the written UPPER_BOUND default wins.
 */
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@NullMarked
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.UPPER_BOUND)
// :: error: (conflicting.defaults)
class NullMarkedBetweenRepeatedDefaultQualifiers<T> {
    Object f = null;

    // UPPER_BOUND is @Nullable, from the written @DefaultQualifier; no error here.
    void use(NullMarkedBetweenRepeatedDefaultQualifiers<@Nullable String> p) {}
}
