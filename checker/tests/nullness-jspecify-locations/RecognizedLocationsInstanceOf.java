// @below-java14-jdk-skip-test

// An unannotated instanceof pattern: not reported just because a pattern is present. Separate
// from RecognizedLocations.java because pattern matching for instanceof requires Java 14+ (as a
// preview feature; finalized in Java 16), matching the same version guard already used for this
// syntax in checker/tests/nullness/java17/NullnessInstanceOf.java.

public class RecognizedLocationsInstanceOf {
    void instanceOf(Object o) {
        if (o instanceof String[] unannotated) {}
    }
}
