import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Order-invariance regression test for intersection-type bounds: this file and
// IntersectionBoundOrderB.java contain the same declarations, but with the
// intersection bounds and the class members in the opposite order. Both files
// must produce identical diagnostics. Before the greatest-lower-bound fix in
// AnnotatedIntersectionType.copyIntersectionBoundAnnotations, the first
// annotated bound in source order determined the intersection's qualifier, so
// reordering the bounds changed the diagnostics.
public class IntersectionBoundOrderA {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    // The intersection's qualifier is glb(@LubglbB, @LubglbC) = @LubglbD; both
    // explicit annotations differ from it and are flagged.
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbB OrderIfaceA & @LubglbC OrderIfaceB> void call(S p) {}

    void useD(@LubglbD OrderImpl d) {
        call(d);
    }

    void useB(@LubglbB OrderImpl b) {
        // :: error: (type.arguments.not.inferred)
        call(b);
    }

    void useC(@LubglbC OrderImpl c) {
        // :: error: (type.arguments.not.inferred)
        call(c);
    }
}
