import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDMiddle;
import org.checkerframework.framework.testchecker.nontopdefault.qual.NTDTop;

// Capture conversion of Foo<? extends Bar> where Foo<T extends @NTDTop Object> must give the
// captured type variable an upper bound equal to the glb of the wildcard's extends bound and the
// type parameter's bound -- not the type parameter's bound wholesale.  Here the wildcard's extends
// bound (the type-use default @NTDMiddle, or an explicit qualifier) is more precise than the
// @NTDTop parameter bound, so the glb is the wildcard's qualifier.
//
// This fills the coverage gap noted in framework/tests/h1h2checker/WildcardBounds.java ("We could
// add an OuterS1 to also test with a non-top upper bound."): the existing wildcard-capture tests
// use a top parameter bound, for which the glb trivially equals the wildcard's bound.  See also
// cf-tasks/task-3-findings.md.
@SuppressWarnings("inconsistent.constructor.type") // not the point of this test
class CaptureGlbWildcardBound {
    interface Bar {}

    interface Foo<T extends @NTDTop Object> {
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
}
