package customaliasingalias;

/**
 * A field explicitly annotated @Unique using a registered alias must be reported the same way its
 * canonical form would be: AnnotatedTypeMirror#getExplicitAnnotations resolves aliasing, and
 * AliasingVisitor#visitVariable reads it via hasExplicitAnnotation(Class).
 */
public class CustomAliasedUnique {
    // :: error: (unique.location.forbidden)
    @Unique Object f;
}
