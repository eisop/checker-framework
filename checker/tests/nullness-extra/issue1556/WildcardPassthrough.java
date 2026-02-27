import org.checkerframework.framework.qual.AnnotatedFor;

// Has @AnnotatedFor("nullness"), so it is fully checked.
// This represents MostlyNoElementQualifierHierarchy.
// It takes GenericFactory<?> as a parameter.
@AnnotatedFor("nullness")
class WildcardBase {
    WildcardBase(GenericFactory<?> factory) {}
}

// Has @AnnotatedFor("nullness"), so it is fully checked.
// This represents SubtypeIsSubsetQualifierHierarchy.
// It passes GenericFactory<?> to super() -- this is line 43's analog.
// With -AuseConservativeDefaultsForUncheckedCode=source, the wildcard
// bounds of GenericFactory<?> may be computed inconsistently:
// - When GenericFactory.java is processed before this file, the bounds
//   may be annotated one way.
// - When GenericFactory.java is processed after this file (or later in
//   the compiler's internal queue), the bounds may be annotated differently.
// This inconsistency can cause [nullness:argument.type.incompatible] and
// [keyfor:argument.type.incompatible] errors at the super() call.
@AnnotatedFor("nullness")
public class WildcardPassthrough extends WildcardBase {
    public WildcardPassthrough(GenericFactory<?> factory) {
        super(factory); // analog to SubtypeIsSubsetQualifierHierarchy.java:43
    }
}
