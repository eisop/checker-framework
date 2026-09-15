// @NullMarked aliases to @DefaultQualifier(NonNull.class, locations = UPPER_BOUND), so it is the
// one mechanism that can put a synthesized @DefaultQualifier on an element that also carries a
// written @DefaultQualifier (or a written @DefaultQualifier.List, which is what javac produces
// for a repeated @DefaultQualifier).  These classes pin what that combination does.
//
// Among the @DefaultQualifier annotations that apply to a declaration -- written or contributed
// by an alias -- the one appearing first in the source wins; a later conflicting one is reported
// as a conflicting.defaults error and discarded.  Each conflict below therefore appears in both
// orders, and the two orders produce different defaults.
//
// The conflicting classes must show *which* qualifier UPPER_BOUND ends up with, not merely that
// @NullMarked lost: a type parameter's upper bound defaults to @Nullable when no @DefaultQualifier
// applies at all, so a written @DefaultQualifier naming @Nullable could not tell "the written
// annotation won" apart from "both were discarded".  They write @MonotonicNonNull, which is
// neither, and pair two observables:
//
//   - a use of the class with a @Nullable type argument, which is an error unless UPPER_BOUND is
//     @Nullable; and
//   - returning a T where a @NonNull is required, which is an error unless UPPER_BOUND is
//     @NonNull.
//
// Neither error means @Nullable, so no default applied; only the first means @NonNull, so
// @NullMarked won; both mean @MonotonicNonNull, so the written @DefaultQualifier won.

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
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
@DefaultQualifier(value = MonotonicNonNull.class, locations = TypeUseLocation.UPPER_BOUND)
// :: error: (conflicting.defaults)
class NullMarkedBeforeDefaultQualifier<T> {
    // UPPER_BOUND is @NonNull, from @NullMarked: the first observable reports, the second does not.
    // :: error: (type.argument.type.incompatible)
    void use(NullMarkedBeforeDefaultQualifier<@Nullable String> p) {}

    @NonNull Object bound(T t) {
        return t;
    }
}

/**
 * The same two annotations in the other order: the written {@code @DefaultQualifier} is first, so
 * it wins and {@code @NullMarked}'s UPPER_BOUND default is the one discarded.
 */
@DefaultQualifier(value = MonotonicNonNull.class, locations = TypeUseLocation.UPPER_BOUND)
@NullMarked
// :: error: (conflicting.defaults)
class DefaultQualifierBeforeNullMarked<T> {
    // UPPER_BOUND is @MonotonicNonNull, from the written @DefaultQualifier: both observables
    // report, which no other outcome does.
    // :: error: (type.argument.type.incompatible)
    void use(DefaultQualifierBeforeNullMarked<@Nullable String> p) {}

    @NonNull Object bound(T t) {
        // :: error: (return.type.incompatible)
        return t;
    }
}

/**
 * One of the repeated @DefaultQualifier annotations sets UPPER_BOUND, which is exactly what
 * {@code @NullMarked} sets, to a different qualifier. Only one qualifier from a hierarchy can be
 * the default for a location, so this is an error rather than a silent pick. {@code @NullMarked} is
 * first, so it wins.
 */
@NullMarked
@DefaultQualifier(value = MonotonicNonNull.class, locations = TypeUseLocation.UPPER_BOUND)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
// :: error: (conflicting.defaults)
class RepeatedDefaultQualifierConflictingWithNullMarked<T> {
    // The non-conflicting half of the same @DefaultQualifier.List still applies.
    Object f = null;

    // UPPER_BOUND is @NonNull, from @NullMarked.
    // :: error: (type.argument.type.incompatible)
    void use(RepeatedDefaultQualifierConflictingWithNullMarked<@Nullable String> p) {}

    @NonNull Object bound(T t) {
        return t;
    }
}

/**
 * The same repeated @DefaultQualifier annotations, now written before {@code @NullMarked}: the
 * written UPPER_BOUND default wins.
 */
@DefaultQualifier(value = MonotonicNonNull.class, locations = TypeUseLocation.UPPER_BOUND)
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@NullMarked
// :: error: (conflicting.defaults)
class RepeatedDefaultQualifierBeforeNullMarked<T> {
    Object f = null;

    // UPPER_BOUND is @MonotonicNonNull, from the written @DefaultQualifier.
    // :: error: (type.argument.type.incompatible)
    void use(RepeatedDefaultQualifierBeforeNullMarked<@Nullable String> p) {}

    @NonNull Object bound(T t) {
        // :: error: (return.type.incompatible)
        return t;
    }
}

/**
 * {@code @NullMarked} written between two {@code @DefaultQualifier} annotations. javac collapses
 * repeated annotations into a single {@code @DefaultQualifier.List} at the position of the first
 * one, so both written defaults precede {@code @NullMarked} even though the second is textually
 * after it, and the written UPPER_BOUND default wins.
 */
@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)
@NullMarked
@DefaultQualifier(value = MonotonicNonNull.class, locations = TypeUseLocation.UPPER_BOUND)
// :: error: (conflicting.defaults)
class NullMarkedBetweenRepeatedDefaultQualifiers<T> {
    Object f = null;

    // UPPER_BOUND is @MonotonicNonNull, from the written @DefaultQualifier.
    // :: error: (type.argument.type.incompatible)
    void use(NullMarkedBetweenRepeatedDefaultQualifiers<@Nullable String> p) {}

    @NonNull Object bound(T t) {
        // :: error: (return.type.incompatible)
        return t;
    }
}
