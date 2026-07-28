import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Companion to IntersectionBoundOrderA/B.java. Those files cover the case where MULTIPLE bounds
// explicitly constrain the (single) hierarchy, so first-bound-wins keeps the first bound's
// qualifier. This file covers the complementary case: the FIRST bound carries no explicit qualifier
// (it relies on defaulting) and only a LATER bound is explicitly annotated.
//
// First-bound-wins holds uniformly, whether the first bound is annotated explicitly or by
// defaulting. AnnotatedIntersectionType#copyIntersectionBoundAnnotations() lets only the first
// bound introduce a hierarchy into the summary; a hierarchy that only a later bound constrains is
// left out of the summary and filled by the normal defaulting pass with the FIRST bound's own
// default. The first bound and the whole intersection sit at the same defaulting location (the type
// variable's upper bound), so that default is exactly the first bound's value in the hierarchy. So
// the summary is the first bound's default, NOT the later bound's explicit qualifier, and the later
// bound's ignored explicit qualifier draws an explicit.annotation.ignored warning (just as an
// explicitly annotated first bound would cause the later bound to be ignored).
public class IntersectionBoundDefaulting {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    // The first bound (OrderIfaceA) has no explicit qualifier; only the second bound is annotated,
    // with @LubglbC. First-bound-wins makes the summary the first bound's default, which in this
    // hierarchy is @LubglbA (the top, which is also this checker's @DefaultQualifierInHierarchy).
    // The second bound's explicit @LubglbC is therefore ignored, so an explicit.annotation.ignored
    // warning is issued on it. `call` requires its argument to be @LubglbA (or a subtype), i.e. any
    // qualifier in this hierarchy.
    // :: warning: (explicit.annotation.ignored)
    <S extends OrderIfaceA & @LubglbC OrderIfaceB> void call(S p) {}

    void useC(@LubglbC OrderImpl c) {
        call(c);
    }

    void useE(@LubglbE OrderImpl e) {
        // @LubglbE <: @LubglbA, so this is accepted.
        call(e);
    }

    void useTop(@LubglbA OrderImpl a) {
        // The distinguishing case: @LubglbA is the top and is the first bound's default, so it is
        // the summary. The call type-checks, proving the summary is the first bound's default
        // @LubglbA and NOT the second bound's explicit @LubglbC (which would reject @LubglbA).
        call(a);
    }
}
