import org.checkerframework.framework.testchecker.intersectionperelement.quals.A1;
import org.checkerframework.framework.testchecker.intersectionperelement.quals.B1;

// Test for per-element intersection-bound semantics: IntersectionPerElementChecker's type factory
// overrides shouldHomogenizeIntersectionBounds() to return false. Each bound of the intersection
// keeps its own qualifier, so the @A1 on the Number bound does NOT leak onto the CharSequence
// bound and the @B1 on the CharSequence bound does NOT leak onto the Number bound.
//
// Under the default (homogenized) semantics, the intersection's primary annotation {@A1, @B1}
// would be copied onto BOTH bounds, so all four assignments below would type-check. Under
// per-element semantics, lines that rely on a leaked qualifier are errors.
public class IntersectionBounds {

    <T extends @A1 Number & @B1 CharSequence> void m(T t) {
        // The Number bound is @A1 in hierarchy A; its hierarchy-B qualifier is the default @BTop,
        // not @B1. Homogenization would have leaked @B1 onto it.
        // :: error: (assignment.type.incompatible)
        @B1 Number n1 = t;

        // The Number bound is @A1, so this succeeds under both semantics.
        @A1 Number n2 = t;

        // The CharSequence bound is @B1 in hierarchy B; its hierarchy-A qualifier is the default
        // @ATop, not @A1. Homogenization would have leaked @A1 onto it.
        // :: error: (assignment.type.incompatible)
        @A1 CharSequence c1 = t;

        // The CharSequence bound is @B1, so this succeeds under both semantics.
        @B1 CharSequence c2 = t;
    }
}
