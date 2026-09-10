// @below-java21-jdk-skip-test

// Test the locations at which JSpecify gives a nullness annotation no meaning that require Java 21
// pattern matching to exercise: any component in a pattern, whether reached through instanceof or
// a switch label, including inside a nested deconstruction pattern.
//
// A pattern binds a variable, so the root of that variable's type is about a reference the test
// examines and is reported by the always-on instanceof.nullable, while a nested component of it
// refines the bound variable and is reported only under -AjspecifyUnrecognizedLocations, which
// gives no meaning to any component of a pattern.
//
// Uses the canonical annotation, not the org.jspecify.annotations alias used in
// UnrecognizedLocations.java: instanceof.nullable, the diagnostic for the root of a pattern's
// type, does not resolve aliases, while jspecify.unrecognized.location.pattern resolves either
// form equally well.

import org.checkerframework.checker.nullness.qual.Nullable;

public class UnrecognizedLocationsJava21 {

    record Box(Object contents) {}

    void instanceOfBindingPatternComponent(Object o) {
        // A component of a binding pattern's type: it refines the elements of "a", so the
        // Nullness Checker gives it a meaning and only JSpecify mode reports it.
        // :: error: (jspecify.unrecognized.location.pattern)
        if (o instanceof @Nullable String[] a) {}
    }

    void instanceOfBindingPatternRoot(Object o) {
        // The root of a binding pattern's type is about the tested reference itself, so it is
        // reported whether or not -AjspecifyUnrecognizedLocations was supplied.
        // :: error: (instanceof.nullable)
        if (o instanceof @Nullable String a) {}
        // :: error: (instanceof.nullable)
        if (o instanceof String @Nullable [] a) {}
    }

    void instanceOfDeconstructionPattern(Object o) {
        // The root of a nested binding's type: null matches no nested type pattern either, so
        // this is reported like a plain instanceof.
        // :: error: (instanceof.nullable)
        if (o instanceof Box(@Nullable String s)) {}
        // A component of a nested binding's type refines "s", so only JSpecify mode reports it.
        // :: error: (jspecify.unrecognized.location.pattern)
        if (o instanceof Box(@Nullable String[] s)) {}
    }

    void switchBindingPattern(Object o) {
        switch (o) {
            // A pattern in a switch label carries the same rule as an instanceof pattern, but has
            // no existing CF-specific diagnostic to reuse.
            // :: error: (jspecify.unrecognized.location.pattern)
            case @Nullable String[] a -> {}
            default -> {}
        }
    }

    void switchDeconstructionPattern(Object o) {
        switch (o) {
            // :: error: (jspecify.unrecognized.location.pattern)
            case Box(@Nullable String s) -> {}
            default -> {}
        }
    }
}
