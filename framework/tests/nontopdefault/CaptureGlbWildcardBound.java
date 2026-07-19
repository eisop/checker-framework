import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDTop;

// Capture conversion of Foo<? extends Bar> where Foo<T extends @Q Object> must give the captured
// type variable an upper bound equal to the glb of the wildcard's extends bound and the
// (substituted) type parameter bound -- not either bound wholesale.  This mirrors JSpecify's
// spec-grounded capture rule (JLS 5.1.10 glb) with the qualifier lattice
// @NTDBottom <: @NTDMiddle <: @NTDTop, whose non-top type-use default @NTDMiddle plays the role of
// JSpecify's "unspecified".
//
// This fills the coverage gap noted in framework/tests/h1h2checker/WildcardBounds.java ("We could
// add an OuterS1 to also test with a non-top upper bound."): the existing wildcard-capture tests
// use a top parameter bound, for which the glb trivially equals the wildcard's bound.
//
// Round-2 note: an investigation of the JSpecify capture-glb records (cf-tasks2/task-1-findings.md)
// confirmed with instrumentation that AnnotatedTypeFactory.applyCaptureConversion already computes
// the correct qualifier glb in every observed case; the remaining reference-checker mismatches are
// downstream (substituteTypeVariable and a capture-id formatting difference), not capture-side.
// This test locks in the CF behavior that investigation verified correct.
@SuppressWarnings("inconsistent.constructor.type") // not the point of this test
class CaptureGlbWildcardBound {
    interface Bar {}

    interface Foo<T extends @NTDTop Object> {
        T get();
    }

    interface FooMid<T extends @NTDMiddle Object> {
        T get();
    }

    // Implicit wildcard extends bound: Bar defaults to the type-use default @NTDMiddle, which is
    // below the @NTDTop parameter bound.  glb(@NTDMiddle Bar, @NTDTop Object) == @NTDMiddle Bar, so
    // x.get() is @NTDMiddle.
    void implicitBound(Foo<? extends Bar> x) {
        @NTDMiddle Object m = x.get();
    }

    // Explicit @NTDMiddle wildcard extends bound: same glb, @NTDMiddle.
    void explicitMiddleBound(Foo<? extends @NTDMiddle Bar> x) {
        @NTDMiddle Object m = x.get();
    }

    // Discriminating control: an explicit @NTDTop wildcard extends bound equals the parameter
    // bound, so the glb is @NTDTop, which is not assignable to @NTDMiddle.  This confirms the glb
    // machinery actually varies with the wildcard's extends bound.
    void explicitTopBound(Foo<? extends @NTDTop Bar> x) {
        // :: error: (assignment.type.incompatible)
        @NTDMiddle Object m = x.get();
    }

    // The parameter bound constrains a less-precise wildcard bound: with a @NTDMiddle parameter
    // bound, a @NTDTop wildcard extends bound is pulled down by the glb to @NTDMiddle, so x.get()
    // is @NTDMiddle (not the wildcard's @NTDTop).  A no-error assertion here holds only if the glb
    // is taken from both sides.
    void midParamTopWildcard(FooMid<? extends @NTDTop Bar> x) {
        @NTDMiddle Object m = x.get();
    }

    // Both sides @NTDMiddle (the implicit default): glb is @NTDMiddle.
    void midParamImplicit(FooMid<? extends Bar> x) {
        @NTDMiddle Object m = x.get();
    }
}
