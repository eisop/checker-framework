// Checks that read an annotation from a tree must resolve aliases first.  Written with JSpecify's
// @Nullable, which is an alias for the Checker Framework's.

import org.jspecify.annotations.Nullable;

class AliasedAnnotationChecksBase {}

// :: error: (annotation.on.supertype)
public class AliasedAnnotationChecks extends @Nullable AliasedAnnotationChecksBase {

    void instanceOf(Object o) {
        // :: error: (instanceof.nullable)
        boolean b = o instanceof @Nullable String;
    }

    void instanceOfNonNull(Object o) {
        // :: warning: (instanceof.nonnull.redundant)
        boolean b = o instanceof @org.jspecify.annotations.NonNull String;
    }
}
