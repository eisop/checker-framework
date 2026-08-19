import org.checkerframework.framework.testchecker.typedeclbounds.quals.Bottom;
import org.checkerframework.framework.testchecker.typedeclbounds.quals.S1;

class CaptureTypeVarBound {

    // The type-parameter bound is itself an (annotated) type variable, @S1 A.
    interface HolderTV<A, U extends @S1 A> {
        U get();
    }

    <T extends @Bottom Object> void typeVariableBound(HolderTV<T, ? extends @S1 T> h) {
        // Capture UB is glb(@S1 T, @S1 T) = @S1 T; @S1 is NOT a subtype of @Bottom, so this
        // assignment must be flagged.
        // :: error: (assignment.type.incompatible)
        @Bottom Object x = h.get();
    }

    // Contrast: the type-parameter bound is a class, @S1 Object.
    interface HolderObj<U extends @S1 Object> {
        U get();
    }

    void classBound(HolderObj<? extends @S1 Object> h) {
        // Capture UB is @S1; this assignment must be flagged.
        // :: error: (assignment.type.incompatible)
        @Bottom Object x = h.get();
    }
}
