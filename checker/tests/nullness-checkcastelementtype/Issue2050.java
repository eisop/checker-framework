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

    // 3. Casts between arrays with differing component nullness.  The instanceof form of these
    // is in Issue2050ArrayPatterns.java, which needs a newer JDK; see that file.
    void testArrayToArrayMismatchedComponents(
            @NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Widening component: @NonNull elements cast to @Nullable elements allows writing null
        // through alias.
        // :: warning: (cast.unsafe)
        @Nullable String[] nbls = (@Nullable String[]) nonNullStrings;

        // Narrowing component: @Nullable elements cast to @NonNull elements allows reading null as
        // non-null.
        // :: warning: (cast.unsafe)
        @NonNull String[] nns = (@NonNull String[]) nullableStrings;
    }

    // 4. Safe array casts where component nullness is preserved.
    void testSafeArrayCast(@NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Upcast to Object[] with identical component nullness (@NonNull)
        @NonNull Object[] objs1 = (@NonNull Object[]) nonNullStrings;

        // Upcast to Object[] with identical component nullness (@Nullable)
        @Nullable Object[] objs2 = (@Nullable Object[]) nullableStrings;
    }
}
