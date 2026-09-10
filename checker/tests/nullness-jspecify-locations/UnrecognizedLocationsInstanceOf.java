// Test a nullness annotation on a component of the type after instanceof, as opposed to its root.
// Without a pattern, instanceof binds no variable, so such an annotation constrains nothing and is
// reported by instanceof.component whether or not -AjspecifyUnrecognizedLocations was supplied
// (see checker/tests/nullness/java17/NullnessInstanceOf.java for the same cases without the
// option).
//
// Uses the canonical annotation, not the org.jspecify.annotations alias used in
// UnrecognizedLocations.java: the instanceof diagnostics do not resolve aliases (see the existing
// checker/tests/nullness/java17/NullnessInstanceOf.java, which uses the same canonical annotation
// for the same reason).

import org.checkerframework.checker.nullness.qual.Nullable;

public class UnrecognizedLocationsInstanceOf {

    void instanceOfComponent(Object o) {
        // An array's component type is normally a recognized location (see RecognizedLocations),
        // but after instanceof there is no variable whose elements it could constrain. (Generic
        // type arguments cannot be tested this way: "instanceof List<String>" does not compile
        // because the type argument is erased at run time, and an array is reifiable so this check
        // remains legal.)
        // :: error: (instanceof.component)
        if (o instanceof @Nullable String[]) {}
    }

    void instanceOfRoot(Object o) {
        // The root of the tested type is about the tested reference itself.
        // :: error: (instanceof.nullable)
        if (o instanceof @Nullable String) {}
        // :: error: (instanceof.nullable)
        if (o instanceof String @Nullable []) {}
    }
}
