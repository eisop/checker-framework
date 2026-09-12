import viewpointtest.quals.*;

/**
 * Substituting an actual type argument for a formal type variable must not overwrite the actual's
 * own bounds with the formal's bounds.
 */
public class NonAdaptedTypeVariableBound {
    // C's bound is @A. Only @ReceiverDependentQual is receiver-dependent, so viewpoint adaptation
    // is the identity on C's bounds no matter what the receiver is.
    static class PlainBound<C extends @A Object> {
        C get() {
            return null;
        }
    }

    // U's own bound is @B. Substituting U for C must leave that bound alone; if C's @A bound were
    // copied onto U, p.get() would have type `U extends @A Object` and the two assignments below
    // would report the opposite results.
    // :: error: (type.argument.type.incompatible)
    <U extends @B Object> void nonAdaptedBoundKeepsActualBound(@A PlainBound<U> p) {
        @B Object keepsItsOwnBound = p.get();
        // :: error: (assignment.type.incompatible)
        @A Object notTheFormalBound = p.get();
    }
}
