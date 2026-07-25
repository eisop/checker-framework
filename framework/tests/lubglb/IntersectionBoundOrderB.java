import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Order-dependence documentation for intersection-type bounds: this file and
// IntersectionBoundOrderA.java contain the same declarations, but with the
// intersection bounds and the class members in the opposite order, and they
// intentionally produce different diagnostics. See IntersectionBoundOrderA.java.
public class IntersectionBoundOrderB {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    void useC(@LubglbC OrderImpl c) {
        call(c);
    }

    void useB(@LubglbB OrderImpl b) {
        // :: error: (type.arguments.not.inferred)
        call(b);
    }

    void useD(@LubglbD OrderImpl d) {
        call(d);
    }

    // The intersection's qualifier is that of the first bound, @LubglbC; the
    // second bound's @LubglbB differs from it and is flagged.
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbC OrderIfaceB & @LubglbB OrderIfaceA> void call(S p) {}
}
