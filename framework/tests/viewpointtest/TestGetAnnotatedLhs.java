import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.ReceiverDependentQual;
import viewpointtest.quals.Top;

@ReceiverDependentQual class TestGetAnnotatedLhs {
    @ReceiverDependentQual Object f;

    @ReceiverDependentQual TestGetAnnotatedLhs() {
        this.f = new @ReceiverDependentQual Object();
    }

    // This method could be called by both @A and @B instances.
    void recieverDependentMethod(@ReceiverDependentQual TestGetAnnotatedLhs this) {}

    // This method could only be called by @A instances.
    void aMethod(@A TestGetAnnotatedLhs this) {}

    // This method could only be called by @B instances.
    void bMethod(@B TestGetAnnotatedLhs this) {}

    void topWithRefinement() {
        TestGetAnnotatedLhs a = new @A TestGetAnnotatedLhs();
        TestGetAnnotatedLhs b = new @B TestGetAnnotatedLhs();
        a.recieverDependentMethod();
        b.recieverDependentMethod();
        a.aMethod();
        // :: error: (method.invocation.invalid)
        a.bMethod();
        // :: error: (method.invocation.invalid)
        b.aMethod();
        b.bMethod();
        // :: error: (new.class.type.invalid)
        TestGetAnnotatedLhs top = new @Top TestGetAnnotatedLhs();
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

    void topWithoutRefinement() {
        // :: error: (new.class.type.invalid)
        TestGetAnnotatedLhs top = new @Top TestGetAnnotatedLhs();
        // :: error: (assignment.type.incompatible)
        top.f = new @B Object();
        // :: error: (assignment.type.incompatible)
        top.f = new @A Object();
    }
}
