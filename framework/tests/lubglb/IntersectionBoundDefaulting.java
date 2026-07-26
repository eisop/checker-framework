import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Companion to IntersectionBoundOrderA/B.java. Those files cover the case where MULTIPLE bounds
// explicitly constrain the (single) hierarchy, so first-bound-wins keeps the first bound's
// qualifier. This file covers the complementary, subtle case: the FIRST bound carries no explicit
// qualifier (it relies on defaulting) and only a LATER bound is explicitly annotated.
//
// AnnotatedIntersectionType#copyIntersectionBoundAnnotations() computes the per-hierarchy summary
// from the annotations that are present on each bound when it runs, which are the explicitly
// written ones; a bound's per-hierarchy default is applied only afterward. So a hierarchy that only
// a later bound constrains takes that later bound's explicit qualifier -- NOT the first bound's
// would-be default. This is the sound choice: the intersection IS a @LubglbC OrderIfaceB, so its
// qualifier must be a subtype of @LubglbC; the first bound's implicit default @LubglbA (the top) is
// not a subtype of @LubglbC and would be an unsound summary. "First-bound-wins" therefore governs
// only hierarchies that more than one bound explicitly constrains (see IntersectionBoundOrderA);
// it does not promote a first bound's would-be default over a later bound's explicit annotation.
public class IntersectionBoundDefaulting {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    // The first bound (OrderIfaceA) has no explicit qualifier; only the second bound is annotated,
    // with @LubglbC. The intersection's summary is @LubglbC, so `call` requires its argument to be
    // @LubglbC (or a subtype). No explicit.annotation.ignored warning is issued, because the first
    // bound has no explicit qualifier to be ignored.
    <S extends OrderIfaceA & @LubglbC OrderIfaceB> void call(S p) {}

    void useC(@LubglbC OrderImpl c) {
        call(c);
    }

    void useE(@LubglbE OrderImpl e) {
        // @LubglbE <: @LubglbC, so this is accepted.
        call(e);
    }

    void useTop(@LubglbA OrderImpl a) {
        // The distinguishing case: @LubglbA is the top and is the first bound's would-be default.
        // It is rejected, proving the summary is the second bound's @LubglbC and NOT the first
        // bound's default @LubglbA. If first-bound-wins had promoted the first bound's default,
        // this call would type-check.
        // :: error: (type.arguments.not.inferred)
        call(a);
    }
}
