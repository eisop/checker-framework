package custom.alias;

/**
 * A constructor's return type explicitly annotated with a commitment qualifier, written using a
 * registered alias, must be reported the same way its canonical form would be:
 * InitializationVisitor#processMethodTree reads the annotations via the javacutil-level
 * AnnotationUtils#getExplicitAnnotationsOnConstructorResult, which does not resolve aliasing, so
 * the caller must.
 */
public class CustomAliasedConstructorReturnType {
    // :: error: (initialization.invalid.constructor.return.type)
    @Initialized CustomAliasedConstructorReturnType() {}
}
