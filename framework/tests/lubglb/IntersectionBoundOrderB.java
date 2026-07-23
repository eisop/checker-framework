import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Order-invariance regression test for intersection-type bounds: this file and
// IntersectionBoundOrderA.java contain the same declarations, but with the
// intersection bounds and the class members in the opposite order. Both files
// must produce identical diagnostics. See IntersectionBoundOrderA.java.
public class IntersectionBoundOrderB {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    void useC(@LubglbC OrderImpl c) {
        // :: error: (type.arguments.not.inferred)
        call(c);
    }

    void useB(@LubglbB OrderImpl b) {
        // :: error: (type.arguments.not.inferred)
        call(b);
    }

    void useD(@LubglbD OrderImpl d) {
        call(d);
    }

    // The intersection's qualifier is glb(@LubglbC, @LubglbB) = @LubglbD; both
    // explicit annotations differ from it and are flagged.
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbC OrderIfaceB & @LubglbB OrderIfaceA> void call(S p) {}
}
