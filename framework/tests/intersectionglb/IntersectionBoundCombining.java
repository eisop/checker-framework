import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbA;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;

// This checker overrides AnnotatedTypeFactory.combineIntersectionBoundAnnotationsInHierarchy to
// return the greatest lower bound, so an intersection type's summary is the GLB of its bounds'
// qualifiers instead of the first bound's qualifier. Every bound has a say, whether its qualifier
// is written on it or is its own default: each bound is defaulted independently before the bounds
// are summarized, so a bare bound's own default (location-based or, e.g., @DefaultQualifierForUse)
// takes part in combining exactly like a written qualifier. The companion file
// framework/tests/lubglb/IntersectionBoundDefaulting.java covers the bare-bound declarations under
// the default (first-bound-wins) hook.
//
// Hierarchy: @LubglbA is the top and the default qualifier; @LubglbB and @LubglbC are its
// subtypes; @LubglbD is a subtype of both, so glb(@LubglbB, @LubglbC) is @LubglbD.
public class IntersectionBoundCombining {

    interface IfaceA {}

    interface IfaceB {}

    // Uses of IfaceC default to @LubglbC rather than to the top @LubglbA.
    @DefaultQualifierForUse(LubglbC.class)
    interface IfaceC {}

    static class Impl implements IfaceA, IfaceB, IfaceC {}

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

    // The first bound is bare; its own default is the top @LubglbA (IfaceA has no
    // @DefaultQualifierForUse). That default takes part in combining, so the summary is
    // glb(@LubglbA, @LubglbC) = @LubglbC. Under first-bound-wins the summary would be the first
    // bound's default @LubglbA and the written @LubglbC would be ignored.
    <S extends IfaceA & @LubglbC IfaceB> void bareFirstBound(S p) {}

    void useBareFirstBound(@LubglbC Impl c, @LubglbA Impl a) {
        bareFirstBound(c);
        // :: error: (type.arguments.not.inferred)
        bareFirstBound(a);
    }

    // The later bound is bare, and its own default comes from @DefaultQualifierForUse rather than
    // from its location: @LubglbC. That default takes part in combining just as a location default
    // does, so the summary is glb(@LubglbB, @LubglbC) = @LubglbD.
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbB IfaceA & IfaceC> void bareLaterBound(S p) {}

    void useBareLaterBound(@LubglbD Impl d, @LubglbB Impl b) {
        bareLaterBound(d);
        // :: error: (type.arguments.not.inferred)
        bareLaterBound(b);
    }

    // @LubglbB and @LubglbC are true siblings: neither is a subtype of the other, so neither
    // bound's own qualifier alone reaches @LubglbD. Reading the type parameter back out (rather
    // than inferring it from an argument, as the tests above do) is still accepted for @LubglbD,
    // because the summary glb(@LubglbB, @LubglbC) is @LubglbD; a checker that instead checked each
    // bound's own qualifier individually would have to reject this. @LubglbE is unrelated to
    // @LubglbD (it is under @LubglbC only), so it is rejected.
    // :: warning: (explicit.annotation.ignored) :: warning: (explicit.annotation.ignored)
    <T extends @LubglbB IfaceA & @LubglbC IfaceB> void siblingBounds(T t) {
        @LubglbD Object accepted = t;
        // :: error: (assignment.type.incompatible)
        @LubglbE Object rejected = t;
    }

    // Regression test: an F-bounded intersection bound (a bound that mentions the enclosing type
    // parameter's own declaration) must not crash. Defaulting Recursive's own bare bound used to
    // re-enter construction of the same type parameter's upper bound. The first bound's own
    // default is the top @LubglbA (Recursive has no @DefaultQualifierForUse); glb(@LubglbA,
    // @LubglbB) is @LubglbB, so the written qualifier is not ignored -- it already is the summary.
    // This is unrelated to GLB specifically -- see
    // framework/tests/lubglb/IntersectionBoundDefaulting.java for the same case under the default
    // hook -- but is included here too since this checker exercises the summarizing code path
    // most thoroughly.
    static class Recursive<T extends Recursive<T> & @LubglbB IfaceA> {}
}
