// Intentionally no @AnnotatedFor("nullness").
// This represents GenericAnnotatedTypeFactory (which also lacks @AnnotatedFor).
// When compiled with -AuseConservativeDefaultsForUncheckedCode=source, uses of
// GenericFactory<?> in @AnnotatedFor-annotated files may have their wildcard
// bounds computed inconsistently depending on the compilation order.
public abstract class GenericFactory<V extends GenericFactory<V>> {}
