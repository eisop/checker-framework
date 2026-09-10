// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Nullness annotations in a deconstruction pattern, without -AjspecifyUnrecognizedLocations.  A
// nested pattern binds a variable, and per JLS 14.30.2 null matches no type pattern, nested or
// not, so an annotation on the root of such a variable's type is checked like one on the root of
// the type after a plain instanceof.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessInstanceOfPattern {

    record Box(Object contents) {}

    void unannotated(Object o) {
        if (o instanceof Box(String s)) {}
    }

    // The root of a nested binding's type: asserting that a matched component may be null
    // contradicts the test.
    void nestedRoot(Object o) {
        // :: error: (instanceof.nullable)
        if (o instanceof Box(@Nullable String s)) {}
        // :: warning: (instanceof.nonnull.redundant)
        if (o instanceof Box(@NonNull String s)) {}
    }

    // The root of a nested binding's array type is written after the component type.
    void nestedArrayRoot(Object o) {
        // :: error: (instanceof.nullable)
        if (o instanceof Box(String @Nullable [] s)) {}
    }

    // A component of a nested binding's type constrains the elements of the bound variable, so it
    // is meaningful and is not reported.
    void nestedComponent(Object o) {
        if (o instanceof Box(@Nullable String[] s)) {
            // :: error: (dereference.of.nullable)
            System.out.println(s[0].length());
        }
    }
}
