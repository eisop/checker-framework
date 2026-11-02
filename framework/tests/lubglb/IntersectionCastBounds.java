import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;

// In a cast to an intersection type, a bound without a written qualifier gets the cast operand's
// qualifier, as the target of a cast to a non-intersection type does. Only then are the bounds
// summarized, so first-bound-wins holds whether or not the first bound has a written qualifier.
// framework/tests/intersectionglb/IntersectionCastBounds.java covers the same casts under a hook
// that combines bounds by their greatest lower bound.
public class IntersectionCastBounds {

    interface IfaceA {}

    interface IfaceB {}

    void bareFirstBound(@LubglbD Object d) {
        // The first bound gets the operand's @LubglbD, which wins over the later @LubglbB.
        // :: warning: (explicit.annotation.ignored)
        @LubglbD Object x = (IfaceA & @LubglbB IfaceB) d;
    }

    void writtenFirstBound(@LubglbD Object d) {
        // The first bound's written @LubglbB wins over the operand's @LubglbD on the later bound.
        @LubglbB Object x = (@LubglbB IfaceA & IfaceB) d;
        // :: error: (assignment.type.incompatible)
        @LubglbD Object y = (@LubglbB IfaceA & IfaceB) d;
    }

    void bareBounds(@LubglbD Object d) {
        @LubglbD Object x = (IfaceA & IfaceB) d;
    }
}
