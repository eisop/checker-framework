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
        // When checking the below assignment, GenericAnnotatedTypeFactory#getAnnotatedTypeLhs()
        // will be called to get the type of the lhs tree (top.f).
        // Previously this method will completely disable flow refinment, and top.f will have type
        // @Top and thus accept the below assingment. But we should reject it, as top
        // is refined to be @A before. As long as top is still pointing to the @A object, top.f
        // will have type @A, which is not the supertype of @B.
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
