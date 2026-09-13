// @below-java17-jdk-skip-test
// Test case for issue 2050: Unsoundness in array casts and instanceof patterns regarding component
// nullness.
// https://github.com/eisop/checker-framework/issues/2050

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2050 {

    // 1. Narrowing from Object to array with unannotated (default NonNull) elements.
    void testNarrowFromObjectToNonNullArray() {
        @Nullable String[] a = new @Nullable String[] {null};
        Object o = a;

        // :: warning: (instanceof.pattern.unsafe)
        if (o instanceof String[] nns) {
            nns[0].toString();
        }

        // :: warning: (cast.unsafe)
        String[] nns2 = (String[]) o;
        nns2[0].toString();
    }

    // 2. Narrowing from Object to array with explicit @Nullable elements.
    void testNarrowFromObjectToNullableArray() {
        @NonNull String[] s = new String[] {"a"};
        Object o = s;

        // :: warning: (instanceof.pattern.unsafe)
        if (o instanceof @Nullable String[] nbls) {
            nbls[0] = null;
        }

        // :: warning: (cast.unsafe)
        @Nullable String[] nbls2 = (@Nullable String[]) o;
        nbls2[0] = null;
    }

    // 3. Cast/pattern between arrays with differing component nullness.
    void testArrayToArrayMismatchedComponents(
            @NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Widening component: @NonNull elements cast to @Nullable elements allows writing null
        // through alias.
        // :: warning: (cast.unsafe)
        @Nullable String[] nbls = (@Nullable String[]) nonNullStrings;

        // :: warning: (instanceof.pattern.unsafe)
        if (nonNullStrings instanceof @Nullable String[] p1) {}

        // Narrowing component: @Nullable elements cast to @NonNull elements allows reading null as
        // non-null.
        // :: warning: (cast.unsafe)
        @NonNull String[] nns = (@NonNull String[]) nullableStrings;

        // :: warning: (instanceof.pattern.unsafe)
        if (nullableStrings instanceof String[] p2) {}
    }

    // 4. Safe array casts/patterns where component nullness is preserved.
    void testSafeArrayCast(@NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Upcast to Object[] with identical component nullness (@NonNull)
        @NonNull Object[] objs1 = (@NonNull Object[]) nonNullStrings;

        if (nonNullStrings instanceof @NonNull Object[] p1) {}

        // Upcast to Object[] with identical component nullness (@Nullable)
        @Nullable Object[] objs2 = (@Nullable Object[]) nullableStrings;

        if (nullableStrings instanceof @Nullable Object[] p2) {}
    }
}
