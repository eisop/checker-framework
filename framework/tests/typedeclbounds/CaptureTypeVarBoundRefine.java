import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.Top;

// Lattice: @Bottom <: @S1 <: @Top. Default/top = @Top; lower-bound default = @Bottom.
class CaptureTypeVarBoundRefine {

    // Outer type parameter T whose bound carries the TOP qualifier.
    static class Outer<T extends @Top Object> {
        // Inner's declared bound is the (bare) type variable T.
        class Inner<U extends T> {
            U get() {
                throw new RuntimeException();
            }
        }

        // Wildcard extends bound (@Bottom) is TIGHTER than T's bound (@Top).
        // Capture UB = glb(@Bottom Object, T) = @Bottom (bounded above by @Bottom Object).
        // So p.get() should be @Bottom, and the assignment to @Bottom must be OK.
        void tighterWildcard(Inner<? extends @Bottom Object> p) {
            // Should be NO error. If the capture reads as @Top (T's declared bound), a
            // spurious (assignment.type.incompatible) is reported here.
            @Bottom Object x = p.get();
        }

        // Contrast: wildcard bound equals T's bound (@Top). Capture is @Top, so assigning
        // to @Bottom MUST be flagged. This one is expected to error today.
        void topWildcard(Inner<? extends @Top Object> p) {
            // :: error: (assignment.type.incompatible)
            @Bottom Object x = p.get();
        }
    }

    // WORKING contrast (class bound, no type variable): parameter bound is @Top Object.
    static class HolderClassBound<U extends @Top Object> {
        U get() {
            throw new RuntimeException();
        }
    }

    void classBoundTighter(HolderClassBound<? extends @Bottom Object> p) {
        // Capture UB = glb(@Bottom Object, @Top Object) = @Bottom; assignment OK.
        @Bottom Object x = p.get();
    }
}
