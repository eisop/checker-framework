import org.checkerframework.framework.testchecker.intersectionperelement.quals.A1;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.ATop;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.B1;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.BTop;

// Test for per-element intersection-bound semantics: IntersectionPerElementChecker's type factory
// overrides shouldHomogenizeIntersectionBounds() to return false. Each bound of the intersection
// keeps its own qualifier, so the @A1 on the Number bound does NOT leak onto the CharSequence
// bound and the @B1 on the CharSequence bound does NOT leak onto the Number bound.
//
// Under the default (homogenized) semantics, the intersection's primary annotation {@A1, @B1}
// would be copied onto BOTH bounds, so all four "leak" assignments below would type-check. Under
// per-element semantics, lines that rely on a leaked qualifier are errors.
//
// A bound that the source annotates in only one hierarchy still carries exactly one annotation in
// every hierarchy: its explicit qualifier where written, and the implicit-upper-bound default (the
// hierarchy's top) elsewhere. So the @A1 Number bound is @A1 in hierarchy A and @BTop in hierarchy
// B, and the @B1 CharSequence bound is @B1 in hierarchy B and @ATop in hierarchy A. The assignments
// below assert both the non-leak behavior and these defaulted values.
public class IntersectionBounds {

    <T extends @A1 Number & @B1 CharSequence> void m(T t) {
        // The Number bound is @A1 in hierarchy A; its hierarchy-B qualifier is the default @BTop,
        // not @B1. Homogenization would have leaked @B1 onto it.
        // :: error: (assignment.type.incompatible)
        @B1 Number n1 = t;

        // The Number bound is @A1, so this succeeds under both semantics.
        @A1 Number n2 = t;

        // Positively assert the defaulted hierarchy-B qualifier of the Number bound: it is exactly
        // @BTop (the implicit-upper-bound default), so spelling out @A1 @BTop matches the bound and
        // type-checks. If that hierarchy were left empty this would fail to resolve or misbehave.
        @A1
        @BTop
        Number n3 = t;

        // The CharSequence bound is @B1 in hierarchy B; its hierarchy-A qualifier is the default
        // @ATop, not @A1. Homogenization would have leaked @A1 onto it.
        // :: error: (assignment.type.incompatible)
        @A1 CharSequence c1 = t;

        // The CharSequence bound is @B1, so this succeeds under both semantics.
        @B1 CharSequence c2 = t;

        // Positively assert the defaulted hierarchy-A qualifier of the CharSequence bound: it is
        // exactly @ATop (the implicit-upper-bound default), so spelling out @ATop @B1 matches the
        // bound and type-checks.
        @ATop
        @B1
        CharSequence c3 = t;
    }
}
