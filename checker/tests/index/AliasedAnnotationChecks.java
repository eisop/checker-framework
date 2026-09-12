// Checks that read an annotation from a tree must resolve aliases first.  @PolyIndex and
// @IndexFor are aliases for the Lower Bound Checker's polymorphic and @NonNegative qualifiers.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.PolyIndex;

public class AliasedAnnotationChecks {

    // :: error: (invalid.polymorphic.qualifier)
    static class PolymorphicOnClass<@PolyIndex T> {}

    // String is not a relevant Java type for this checker.
    // :: error: (anno.on.irrelevant)
    @IndexFor("#1") String irrelevant(String other) {
        return "";
    }
}
