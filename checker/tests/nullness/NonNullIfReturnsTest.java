// Tests @NonNullIfReturns, @NonNullIfReturnsTrue, and @NonNullIfReturnsFalse (parameter-level
// conditional postconditions) for both correct refinement and expected errors.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonNullIfReturns;
import org.checkerframework.checker.nullness.qual.NonNullIfReturnsFalse;
import org.checkerframework.checker.nullness.qual.NonNullIfReturnsTrue;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NonNullIfReturnsTest {

    // -----------------------------------------------------------------------
    // @NonNullIfReturns(true) / (false)
    // -----------------------------------------------------------------------

    public static boolean hasLength(@NonNullIfReturns(true) @Nullable String str) {
        return (str != null && !str.isEmpty());
    }

    void useHasLength(@Nullable String s) {
        if (hasLength(s)) {
            @NonNull String n = s;
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull String n = s;
        }
    }

    @Override
    public boolean equals(@NonNullIfReturns(true) @Nullable Object other) {
        return this == other;
    }

    void useEquals(NonNullIfReturnsTest a, @Nullable Object b) {
        if (a.equals(b)) {
            @NonNull Object n = b;
            n.hashCode();
        }
    }

    public static boolean isNull(@NonNullIfReturns(false) @Nullable Object obj) {
        return obj == null;
    }

    void useIsNull(@Nullable Object p) {
        if (!isNull(p)) {
            @NonNull Object n = p;
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    public static boolean bothNonNull(
            @NonNullIfReturns(true) @Nullable Object a,
            @NonNullIfReturns(true) @Nullable Object b) {
        return a != null && b != null;
    }

    void useBothNonNull(@Nullable Object x, @Nullable Object y) {
        if (bothNonNull(x, y)) {
            @NonNull Object nx = x;
            @NonNull Object ny = y;
            nx.hashCode();
            ny.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object nx = x;
            // :: error: (assignment.type.incompatible)
            @NonNull Object ny = y;
        }
    }

    // Checker trusts the annotation; does not verify method body.
    public boolean alwaysTrue(@NonNullIfReturns(true) @Nullable Object obj) {
        return true;
    }

    void useAlwaysTrue(@Nullable Object p) {
        if (alwaysTrue(p)) {
            @NonNull Object n = p;
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    // -----------------------------------------------------------------------
    // @NonNullIfReturnsTrue
    // -----------------------------------------------------------------------

    public static boolean hasLengthTrue(@NonNullIfReturnsTrue @Nullable String str) {
        return (str != null && !str.isEmpty());
    }

    void useHasLengthTrue(@Nullable String s) {
        if (hasLengthTrue(s)) {
            @NonNull String n = s;
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull String n = s;
        }
    }

    public static boolean bothNonNullTrue(
            @NonNullIfReturnsTrue @Nullable Object a, @NonNullIfReturnsTrue @Nullable Object b) {
        return a != null && b != null;
    }

    void useBothNonNullTrue(@Nullable Object x, @Nullable Object y) {
        if (bothNonNullTrue(x, y)) {
            @NonNull Object nx = x;
            @NonNull Object ny = y;
            nx.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object nx = x;
            // :: error: (assignment.type.incompatible)
            @NonNull Object ny = y;
        }
    }

    // -----------------------------------------------------------------------
    // @NonNullIfReturnsFalse
    // -----------------------------------------------------------------------

    public static boolean isNullFalse(@NonNullIfReturnsFalse @Nullable Object obj) {
        return obj == null;
    }

    void useIsNullFalse(@Nullable Object p) {
        if (!isNullFalse(p)) {
            @NonNull Object n = p;
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    public static boolean neitherNull(
            @NonNullIfReturnsFalse @Nullable Object a, @NonNullIfReturnsFalse @Nullable Object b) {
        return a == null || b == null;
    }

    void useNeitherNull(@Nullable Object x, @Nullable Object y) {
        if (!neitherNull(x, y)) {
            @NonNull Object nx = x;
            @NonNull Object ny = y;
            nx.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object nx = x;
            // :: error: (assignment.type.incompatible)
            @NonNull Object ny = y;
        }
    }
}
