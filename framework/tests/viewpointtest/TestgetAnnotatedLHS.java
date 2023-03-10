import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

@ReceiverDependentQual
class TestgetAnnotatedLHS {
    @ReceiverDependentQual Object f;

    @SuppressWarnings({
        "inconsistent.constructor.type",
        "super.invocation.invalid",
        "cast.unsafe.constructor.invocation"
    })
    @ReceiverDependentQual
    TestgetAnnotatedLHS() {
        this.f = new @ReceiverDependentQual Object();
    }

    @SuppressWarnings({"cast.unsafe.constructor.invocation"})
    void topWithRefinement() {
        TestgetAnnotatedLHS a = new @A TestgetAnnotatedLHS();
        TestgetAnnotatedLHS top = new @Top TestgetAnnotatedLHS();
        top = a;
        // :: error: (assignment.type.incompatible)
        top.f = new @B Object();
        top.f = new @A Object(); // no error here
    }

    @SuppressWarnings({"cast.unsafe.constructor.invocation"})
    void topWithoutRefinement() {
        TestgetAnnotatedLHS top = new @Top TestgetAnnotatedLHS();
        top.f = new @A Object();
    }
}
