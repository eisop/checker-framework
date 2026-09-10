// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test the locations at which JSpecify gives a nullness annotation no meaning that require Java 21
// pattern matching to exercise: any component in a pattern, whether reached through instanceof or
// a switch label, including inside a nested deconstruction pattern.
//
// Uses the canonical annotation, not the org.jspecify.annotations alias used in
// UnrecognizedLocations.java: instanceof.nullable, the diagnostic for a pattern reached through
// instanceof (see UnrecognizedLocations.instanceOfAnyComponent), does not resolve aliases, while
// jspecify.unrecognized.location, the diagnostic for a pattern reached through a switch label,
// resolves either form equally well.

import org.checkerframework.checker.nullness.qual.Nullable;

public class UnrecognizedLocationsJava21 {

    record Box(Object contents) {}

    void instanceOfBindingPattern(Object o) {
        // Any component of a binding pattern's type, not only its root. Reported as
        // instanceof.nullable, like a plain (non-pattern) instanceof.
        // :: error: (instanceof.nullable)
        if (o instanceof @Nullable String[] a) {}
    }

    void instanceOfDeconstructionPattern(Object o) {
        // Any component of a nested pattern inside a deconstruction pattern.
        // :: error: (instanceof.nullable)
        if (o instanceof Box(@Nullable String s)) {}
    }

    void switchBindingPattern(Object o) {
        switch (o) {
            // A pattern in a switch label carries the same rule as an instanceof pattern, but has
            // no existing CF-specific diagnostic to reuse.
            // :: error: (jspecify.unrecognized.location)
            case @Nullable String[] a -> {}
            default -> {}
        }
    }

    void switchDeconstructionPattern(Object o) {
        switch (o) {
            // :: error: (jspecify.unrecognized.location)
            case Box(@Nullable String s) -> {}
            default -> {}
        }
    }
}
