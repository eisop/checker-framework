class NestedRecordUsage {

    // NestedRecordOuter.astub annotates the nested record's "value" component as @Nullable, and
    // that annotation must reach the canonical constructor's corresponding parameter. Until
    // AnnotationFileParser learned to descend into a record nested in another type, the record
    // declaration in the annotation file was ignored entirely and this was an
    // argument.type.incompatible error.
    void canonicalConstructorParameterIsNullable() {
        NestedRecordOuter.Inner x = new NestedRecordOuter.Inner("key", null);
    }
}
