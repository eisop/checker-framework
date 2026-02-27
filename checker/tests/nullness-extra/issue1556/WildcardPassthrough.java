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
// GenericFactory has no @AnnotatedFor("nullness"), so the nullness and keyfor
// checkers cannot determine the exact wildcard bounds.  Suppress the resulting
// false-positive argument.type.incompatible errors, matching the pattern used in
// QualifierHierarchy.java and SubtypeIsSubsetQualifierHierarchy.java.
@AnnotatedFor("nullness")
public class WildcardPassthrough extends WildcardBase {
    @SuppressWarnings({"nullness", "keyfor"}) // GenericFactory hasn't been annotated.
    public WildcardPassthrough(GenericFactory<?> factory) {
        super(factory); // analog to SubtypeIsSubsetQualifierHierarchy.java:43
    }
}
