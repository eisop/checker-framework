package custom.alias;

/**
 * A field explicitly annotated with a commitment qualifier, written using a registered alias, must
 * be reported the same way its canonical form would be: InitializationVisitor#visitVariable reads
 * the field's explicit annotations via AnnotatedTypeMirror#getExplicitAnnotations, which resolves
 * aliasing.
 */
public class CustomAliasedFieldType {
    // :: error: (initialization.invalid.field.type)
    @Initialized Object f = new Object();
}
