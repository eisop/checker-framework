package customaliasingalias;

/**
 * A field explicitly annotated @Unique using a registered alias must be reported the same way its
 * canonical form would be: AnnotatedTypeMirror#getExplicitAnnotations resolves aliasing, and
 * AliasingVisitor#visitVariable reads it via hasExplicitAnnotation(Class).
 */
public class CustomAliasedUnique {
    // Fully qualified: under -AajavaChecks on JDK < 21, TypeAnnotationMover looks up a simple name
    // in the unnamed package, which re-reads Unique.java via -sourcepath ("duplicate class").
    // :: error: (unique.location.forbidden)
    @customaliasingalias.Unique Object f;
}
