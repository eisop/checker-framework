import org.checkerframework.framework.testchecker.lubglb.quals.*;

// Order-dependence documentation for intersection-type bounds: this file and
// IntersectionBoundOrderB.java contain the same declarations, but with the
// intersection bounds and the class members in the opposite order, and they
// intentionally produce different diagnostics. When two bounds carry
// conflicting qualifiers, the intersection's qualifier is the qualifier of the
// first annotated bound in source order (first-bound-wins), so reordering the
// bounds changes which arguments are accepted. This source-order dependence is
// accepted, expected behavior; a checker wanting an order-independent rule (for
// example the greatest lower bound) overrides
// AnnotatedTypeFactory.combineIntersectionBoundAnnotationsInHierarchy.
public class IntersectionBoundOrderA {

    interface OrderIfaceA {}

    interface OrderIfaceB {}

    static class OrderImpl implements OrderIfaceA, OrderIfaceB {}

    // The intersection's qualifier is that of the first bound, @LubglbB; the
    // second bound's @LubglbC differs from it and is flagged.
    // :: warning: (explicit.annotation.ignored)
    <S extends @LubglbB OrderIfaceA & @LubglbC OrderIfaceB> void call(S p) {}

    void useD(@LubglbD OrderImpl d) {
        call(d);
    }

    void useB(@LubglbB OrderImpl b) {
        call(b);
    }

    void useC(@LubglbC OrderImpl c) {
        // :: error: (type.arguments.not.inferred)
        call(c);
    }
}
