package custom.alias;

public class CustomAliasedAnnotations {

    void useNonNullAnnotations() {
        // :: error: (assignment.type.incompatible)
        @org.checkerframework.checker.nullness.qual.NonNull Object nn1 = null;
        // :: error: (assignment.type.incompatible)
        @NonNull Object nn2 = null;
    }

    void useNullableAnnotations1(@org.checkerframework.checker.nullness.qual.Nullable Object nble) {
        // :: error: (dereference.of.nullable)
        nble.toString();
    }

    void useNullableAnnotations2(@Nullable Object nble) {
        // :: error: (dereference.of.nullable)
        nble.toString();
    }
}
