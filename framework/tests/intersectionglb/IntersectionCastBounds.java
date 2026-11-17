import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;

// In a cast to an intersection type, a bound without a written qualifier gets the cast operand's
// qualifier, so it takes part in the greatest lower bound like a bound with a written qualifier,
// in either position. framework/tests/lubglb/IntersectionCastBounds.java covers the same casts
// under first-bound-wins.
public class IntersectionCastBounds {

    interface IfaceA {}

    interface IfaceB {}

    void bareFirstBound(@LubglbB Object b) {
        // glb(@LubglbB, @LubglbC) = @LubglbD, below the operand's @LubglbB.
        // :: warning: (cast.unsafe)
        @LubglbD Object x = (IfaceA & @LubglbC IfaceB) b;
    }

    void bareLaterBound(@LubglbB Object b) {
        // :: warning: (cast.unsafe)
        @LubglbD Object x = (@LubglbC IfaceA & IfaceB) b;
    }

    void writtenAboveOperand(@LubglbD Object d) {
        // glb(@LubglbD, @LubglbC) = @LubglbD: the written @LubglbC does not lose the operand's
        // more precise qualifier.
        @LubglbD Object x = (IfaceA & @LubglbC IfaceB) d;
        // :: error: (assignment.type.incompatible)
        @LubglbE Object y = (IfaceA & @LubglbC IfaceB) d;
    }
}
