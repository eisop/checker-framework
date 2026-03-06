// Test that @NotNullIfReturns (parameter-level conditional postcondition) is respected by the
// Nullness Checker.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.NotNullIfReturns;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NotNullIfReturnsTest {

    // Example 1: hasLength — param is non-null when method returns true
    public static boolean hasLength(@NotNullIfReturns(true) @Nullable String str) {
        return (str != null && !str.isEmpty());
    }

    void useHasLength(@Nullable String s) {
        if (hasLength(s)) {
            @NonNull String n = s; // OK: s is non-null when hasLength returns true
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull String n = s;
        }
    }

    // Example 2: equals — param is non-null when method returns true
    @Override
    public boolean equals(@NotNullIfReturns(true) @Nullable Object other) {
        return this == other;
    }

    void useEquals(NotNullIfReturnsTest a, @Nullable Object b) {
        if (a.equals(b)) {
            @NonNull Object n = b; // OK: b is non-null when equals returns true
            n.hashCode();
        }
    }

    // Example 3: isNull — param is non-null when method returns false
    public static boolean isNull(@NotNullIfReturns(false) @Nullable Object obj) {
        return obj == null;
    }

    void useIsNull(@Nullable Object p) {
        if (!isNull(p)) {
            @NonNull Object n = p; // OK: p is non-null when isNull returns false
            n.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object n = p;
        }
    }

    // Example 4: Multiple parameters — both non-null when method returns true
    public static boolean bothNonNull(
            @NotNullIfReturns(true) @Nullable Object a,
            @NotNullIfReturns(true) @Nullable Object b) {
        return a != null && b != null;
    }

    void useBothNonNull(@Nullable Object x, @Nullable Object y) {
        if (bothNonNull(x, y)) {
            @NonNull Object nx = x; // OK
            @NonNull Object ny = y; // OK
            nx.hashCode();
            ny.hashCode();
        } else {
            // :: error: (assignment.type.incompatible)
            @NonNull Object nx = x;
            // :: error: (assignment.type.incompatible)
            @NonNull Object ny = y;
        }
    }
}
