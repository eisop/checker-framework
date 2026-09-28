// @below-java21-jdk-skip-test
// The array-to-array instanceof cases of issue 2050, split out of Issue2050.java because they
// need JDK 21.  Each pattern here tests an expression against its own erased type, or against a
// supertype of it, so javac sees an unconditional pattern; that was "expression type ... is a
// subtype of pattern type ..." until JDK 21 allowed it.  Only the qualifiers differ, and javac
// does not see those.  The cast forms of the same scenarios are valid on every supported JDK and
// stay in Issue2050.java.
// https://github.com/eisop/checker-framework/issues/2050

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2050ArrayPatterns {

    // Patterns between arrays with differing component nullness.
    void testArrayToArrayMismatchedComponents(
            @NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Widening component: @NonNull elements viewed as @Nullable elements allows writing null
        // through the alias.
        // :: warning: (instanceof.pattern.unsafe)
        if (nonNullStrings instanceof @Nullable String[] p1) {}

        // Narrowing component: @Nullable elements viewed as @NonNull elements allows reading null
        // as non-null.
        // :: warning: (instanceof.pattern.unsafe)
        if (nullableStrings instanceof String[] p2) {}
    }

    // Safe patterns where component nullness is preserved.
    void testSafeArrayPattern(
            @NonNull String[] nonNullStrings, @Nullable String[] nullableStrings) {
        // Upcast to Object[] with identical component nullness (@NonNull)
        if (nonNullStrings instanceof @NonNull Object[] p1) {}

        // Upcast to Object[] with identical component nullness (@Nullable)
        if (nullableStrings instanceof @Nullable Object[] p2) {}
    }
}
