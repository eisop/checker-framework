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

    interface MyList {}

    // The explicit annotation on the second bound is ignored, because the first bound wins.
    // :: warning: (explicit.annotation.ignored)
    <E extends Object & @Nullable MyList> void intersectionBound(E e) {
        e.toString();
    }

    void instanceOfComponent(Object o) {
        // :: error: (instanceof.component)
        boolean b = o instanceof @Nullable String[];
    }

    void throwsClause()
            throws
                    // :: error: (nullness.on.throws)
                    @Nullable Exception {}

    @interface AnnoMember {
        // :: error: (nullness.on.annotation.member)
        @Nullable String value();

        // :: error: (nullness.on.annotation.member)
        @Nullable String[] arrayValue();
    }
}
