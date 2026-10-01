package customlockalias;

/**
 * A GuardedBy-hierarchy qualifier written using a registered alias must be reported the same way
 * its canonical form would be: AnnotatedTypeMirror#getExplicitAnnotations resolves aliasing, and
 * LockVisitor#visitVariable reads it via hasExplicitAnnotation(Class)/hasExplicitAnnotationRelaxed.
 */
public class CustomAliasedGuardedBy {
    // :: error: (immutable.type.guardedby) :: error: (type.invalid.annotations.on.use)
    @GuardedByUnknown int x;
}
