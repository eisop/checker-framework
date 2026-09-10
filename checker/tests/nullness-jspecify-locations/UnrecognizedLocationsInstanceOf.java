// Test "any component" of the type after instanceof, not only its root, reported under
// -AjspecifyUnrecognizedLocations via the existing instanceof.nullable diagnostic rather than a
// new one.
//
// Uses the canonical annotation, not the org.jspecify.annotations alias used in
// UnrecognizedLocations.java: instanceof.nullable does not resolve aliases (see the existing
// checker/tests/nullness/java17/NullnessInstanceOf.java, which uses the same canonical annotation
// for the same reason).

import org.checkerframework.checker.nullness.qual.Nullable;

public class UnrecognizedLocationsInstanceOf {

    void instanceOfAnyComponent(Object o) {
        // An array's component type is normally a recognized location (see RecognizedLocations),
        // but not after instanceof. (Generic type arguments cannot be tested this way: "instanceof
        // List<String>" does not compile because the type argument is erased at run time, and an
        // array is reifiable so this check remains legal.)
        // :: error: (instanceof.nullable)
        if (o instanceof @Nullable String[]) {}
    }
}
