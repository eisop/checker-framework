import org.checkerframework.framework.qual.DefaultQualifierForUse;
import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Companion to IntersectionBoundOrderA/B.java. Those files cover the case where MULTIPLE bounds
// explicitly constrain the (single) hierarchy, so first-bound-wins keeps the first bound's
// qualifier. This file covers the complementary case: the FIRST bound carries no explicit qualifier
// (it relies on defaulting) and only a LATER bound is explicitly annotated.
//
// First-bound-wins holds uniformly, whether the first bound is annotated explicitly or by
// defaulting, and whether the default is a location default (implicit-upper-bound defaulting,
// the top qualifier here) or a type-based one (@DefaultQualifierForUse): each bound is defaulted
// on its own -- see QualifierDefaults's handling of a type variable's intersection upper bound --
// before the bounds are summarized, so the summary is the first bound's own real qualifier,
// whichever kind of default produced it, and a later bound's ignored explicit qualifier draws an
// explicit.annotation.ignored warning (just as an explicitly annotated first bound would cause
// the later bound to be ignored).
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

    // Uses of DefaultedIface default to @LubglbC, not to the top @LubglbA: a type-based default,
    // not a location-based one. First-bound-wins still holds: the summary is the first bound's
    // own default @LubglbC (not @LubglbA, the intersection's own location default, and not the
    // second bound's explicit @LubglbA), so the second bound's @LubglbA is ignored.
    @DefaultQualifierForUse(LubglbC.class)
    interface DefaultedIface {}

    static class DefaultedImpl implements DefaultedIface, OrderIfaceB {}

    // :: warning: (explicit.annotation.ignored)
    <S extends DefaultedIface & @LubglbA OrderIfaceB> void callTypeBased(S p) {}

    void useTypeBasedC(@LubglbC DefaultedImpl c) {
        callTypeBased(c);
    }

    void useTypeBasedTop(@LubglbA DefaultedImpl a) {
        // :: error: (type.arguments.not.inferred)
        callTypeBased(a);
    }

    // Regression test: an F-bounded intersection bound (a bound that mentions the enclosing type
    // parameter's own declaration) must not crash defaulting. Recursive's own bare bound used to
    // re-enter construction of the same type parameter's upper bound while computing its default.
    // The first bound's own default is the top @LubglbA (Recursive has no
    // @DefaultQualifierForUse); first-bound-wins keeps it over the second bound's explicit
    // @LubglbC.
    // :: warning: (explicit.annotation.ignored)
    static class Recursive<T extends Recursive<T> & @LubglbC OrderIfaceA> {}

    // Regression test: self-reference in the SECOND bound position, not just the first. The
    // enclosing type must be an interface here, since only a first bound may be a class.
    interface RecursiveSecond<T extends @LubglbC OrderIfaceA & RecursiveSecond<T>> {}

    // Regression test: mutually recursive F-bounds across two classes (P's own bound mentions Q,
    // and vice versa), not just a single self-referential class. The first bound (the
    // self/mutually-referential one) is bare, defaulting to the top @LubglbA, so the second
    // bound's explicit @LubglbC is ignored, as in Recursive above.
    static class MutuallyRecursiveP<
            T extends MutuallyRecursiveP<T, U> & OrderIfaceA,
            // :: warning: (explicit.annotation.ignored)
            U extends MutuallyRecursiveQ<T, U> & @LubglbC OrderIfaceB> {}

    static class MutuallyRecursiveQ<
            T extends MutuallyRecursiveP<T, U> & OrderIfaceA,
            // :: warning: (explicit.annotation.ignored)
            U extends MutuallyRecursiveQ<T, U> & @LubglbC OrderIfaceB> {}
}
