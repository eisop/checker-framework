import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;

// This checker overrides AnnotatedTypeFactory.combineIntersectionBoundAnnotationsInHierarchy to
// return the greatest lower bound, so an intersection type's summary is the GLB of two bounds'
// qualifiers instead of the first bound's qualifier, whenever both bounds are explicitly
// annotated in a hierarchy. (A bare bound's own default does not take part in combining; see the
// second bullet of AnnotatedIntersectionType#copyIntersectionBoundAnnotations's Javadoc for why
// that is a known limitation, not addressed here.) The companion files
// framework/tests/lubglb/IntersectionBoundOrderA.java and IntersectionBoundOrderB.java cover the
// same both-explicit declarations under the default (first-bound-wins) hook.
//
// Hierarchy: @LubglbA is the top and the default qualifier; @LubglbB and @LubglbC are its
// subtypes; @LubglbD is a subtype of both, so glb(@LubglbB, @LubglbC) is @LubglbD.
public class IntersectionBoundCombining {

    interface IfaceA {}

    interface IfaceB {}

    static class Impl implements IfaceA, IfaceB {}

    // Both bounds are explicitly annotated, so both take part in combining: the summary is
    // glb(@LubglbB, @LubglbC) = @LubglbD. Neither written qualifier is the summary, so both are
    // reported as ignored.
    // :: warning: (explicit.annotation.ignored) :: warning: (explicit.annotation.ignored)
    <S extends @LubglbB IfaceA & @LubglbC IfaceB> void bothExplicit(S p) {}

    void useBothExplicit(@LubglbD Impl d, @LubglbB Impl b) {
        bothExplicit(d);
        // :: error: (type.arguments.not.inferred)
        bothExplicit(b);
    }

    // @LubglbB and @LubglbC are true siblings: neither is a subtype of the other, so neither
    // bound's own qualifier alone reaches @LubglbD. Reading the type parameter back out (rather
    // than inferring it from an argument, as the test above does) is still accepted for @LubglbD,
    // because the summary glb(@LubglbB, @LubglbC) is @LubglbD; a checker that instead checked each
    // bound's own qualifier individually would have to reject this. @LubglbE is unrelated to
    // @LubglbD (it is under @LubglbC only), so it is rejected.
    // :: warning: (explicit.annotation.ignored) :: warning: (explicit.annotation.ignored)
    <T extends @LubglbB IfaceA & @LubglbC IfaceB> void siblingBounds(T t) {
        @LubglbD Object accepted = t;
        // :: error: (assignment.type.incompatible)
        @LubglbE Object rejected = t;
    }
}
