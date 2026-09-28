// Test that viewpoint-adapted type declaration bounds are adapted in the correct direction.

import viewpointtest.quals.*;

public class AdaptedTypeDeclarationBounds {

    static class DefaultBound {}

    @ReceiverDependentQual interface RdqBound {}

    @A interface ABound {}

    @B interface BBound {}

    @ReceiverDependentQual Object rdqObject;

    @ReceiverDependentQual DefaultBound rdqDefaultBound;

    @ReceiverDependentQual RdqBound rdqAsRdq;
    @A RdqBound rdqAsA;
    @B RdqBound rdqAsB;
    // :: error: (type.invalid.annotations.on.use)
    @Top RdqBound rdqAsTop;

    @A ABound aAsA;
    // :: error: (type.invalid.annotations.on.use)
    @Top ABound aAsTop;
    // :: error: (type.invalid.annotations.on.use)
    @B ABound aAsB;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentQual ABound aAsRdq;

    @B BBound bAsB;
    // :: error: (type.invalid.annotations.on.use)
    @Top BBound bAsTop;
    // :: error: (type.invalid.annotations.on.use)
    @A BBound bAsA;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentQual BBound bAsRdq;

    void use(
            @ReceiverDependentQual Object param, @ReceiverDependentQual DefaultBound defaultBound) {
        @ReceiverDependentQual Object local = param;
        @ReceiverDependentQual DefaultBound localDefaultBound = defaultBound;
    }
}
