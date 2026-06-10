import viewpointtest.quals.*;

public class LostInBounds {
    static class Generic<T extends @ReceiverDependentQual Object> {}

    // Use @Bottom so the type argument is within the adapted @Lost bound. That isolates the
    // diagnostic for @Lost in the adapted bound.
    void testBounds(
            // :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@Bottom Object> generic) {
        // :: error: (viewpointtest.lost.in.bounds)
        @Top Generic<@Bottom Object> local = generic;
    }
}
