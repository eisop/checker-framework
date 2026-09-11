package custom.alias;

/**
 * A field explicitly annotated with a commitment qualifier, written using a registered alias, must
 * be reported the same way its canonical form would be: InitializationVisitor#visitVariable reads
 * the field's explicit annotations via AnnotatedTypeMirror#getExplicitAnnotations, which resolves
 * aliasing.
 */
public class CustomAliasedFieldType {
    // Fully qualified: under -AajavaChecks on JDK < 21, TypeAnnotationMover looks up a simple name
    // in the unnamed package, which re-reads Initialized.java via -sourcepath ("duplicate class").
    // :: error: (initialization.invalid.field.type)
    @custom.alias.Initialized Object f = new Object();
}
