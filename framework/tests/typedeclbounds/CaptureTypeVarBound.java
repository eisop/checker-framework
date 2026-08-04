import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.S1;

class CaptureTypeVarBound {

    // BROKEN: the type-parameter bound is itself an (annotated) type variable, @S1 A.
    interface HolderTV<A, U extends @S1 A> {
        U get();
    }

    <T extends @Bottom Object> void broken(HolderTV<T, ? extends @S1 T> h) {
        // Capture UB should be glb(@S1 T, @S1 T) = @S1 T; @S1 is NOT a subtype of @Bottom,
        // so this assignment must be flagged.
        // BUG: the @S1 on the bound `@S1 A` is dropped during the capture substitution,
        // the capture UB collapses to @Bottom, and NO error is issued.
        // :: error: (assignment.type.incompatible)
        @Bottom Object x = h.get();
    }

    // WORKING contrast: the type-parameter bound is a class, @S1 Object.
    interface HolderObj<U extends @S1 Object> {
        U get();
    }

    void working(HolderObj<? extends @S1 Object> h) {
        // Capture UB is correctly @S1; this assignment is correctly flagged today.
        // :: error: (assignment.type.incompatible)
        @Bottom Object x = h.get();
    }
}
