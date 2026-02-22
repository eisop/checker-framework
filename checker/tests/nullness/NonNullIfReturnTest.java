// Test that @NonNullIfReturn (parameter-level conditional postcondition) is respected by the
// Nullness Checker. Same semantics as @EnsuresNonNullIf(expression="#1", result=true) on the
// method, but written on the parameter.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NonNullIfReturn;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NonNullIfReturnTest {

    // Example 1: Simple null check (equivalent to @EnsuresNonNullIf(expression="#1", result=true))
    public static boolean isNonNull(@NonNullIfReturn(true) @Nullable Object obj) {
        return obj != null;
    }

    void useIsNonNull(@Nullable Object p) {
        if (isNonNull(p)) {
            @NonNull Object n = p; // OK: p is non-null when isNonNull returns true
            n.hashCode(); // use n
        } else {
            // Contract does not apply: when isNonNull returns false, p may be null.
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    // Example 2: equals (parameter is non-null when return is true)
    @Override
    public boolean equals(@NonNullIfReturn(true) @Nullable Object other) {
        return this == other;
    }

    void useEquals(NonNullIfReturnTest a, @Nullable Object b) {
        if (a.equals(b)) {
            @NonNull Object n = b; // OK: b is non-null when equals returns true
            n.hashCode(); // use n
        }
    }

    // Example 3: Negative return value (parameter is non-null when return is false)
    public static boolean isNull(@NonNullIfReturn(false) @Nullable Object obj) {
        return obj == null;
    }

    void useIsNull(@Nullable Object p) {
        if (!isNull(p)) {
            @NonNull Object n = p; // OK: p is non-null when isNull returns false
            n.hashCode(); // use n
        } else {
            // Contract does not apply: when isNull returns true, p is null.
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    // Example 4: Multiple parameters
    public static boolean bothNonNull(
            @NonNullIfReturn(true) @Nullable Object a, @NonNullIfReturn(true) @Nullable Object b) {
        return a != null && b != null;
    }

    void useBothNonNull(@Nullable Object x, @Nullable Object y) {
        if (bothNonNull(x, y)) {
            @NonNull Object nx = x; // OK
            @NonNull Object ny = y; // OK
            nx.hashCode();
            ny.hashCode();
        } else {
            // Contract does not apply: when bothNonNull returns false, x or y may be null.
            // :: error: (assignment.type.incompatible)
            @NonNull Object nx = x;
            // :: error: (assignment.type.incompatible)
            @NonNull Object ny = y;
        }
    }
}
