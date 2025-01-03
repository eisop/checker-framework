import viewpointtest.quals.*;

@ReceiverDependentQual public class LostNonReflexive {
    @ReceiverDependentQual Object f;

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual LostNonReflexive(@ReceiverDependentQual Object args) {}

    @ReceiverDependentQual Object get() {
        return null;
    }

    void set(@ReceiverDependentQual Object o) {}

    void test(@Top LostNonReflexive obj, @Bottom Object bottomObj) {
        // :: error: (assignment.type.incompatible)
        this.f = obj.f;
        this.f = bottomObj;

        // :: error: (assignment.type.incompatible) :: error: (method.invocation.invalid)
        @A Object aObj = obj.get();
        // :: error: (assignment.type.incompatible) :: error: (method.invocation.invalid)
        @B Object bObj = obj.get();
        // :: error: (assignment.type.incompatible) :: error: (method.invocation.invalid)
        @Bottom Object botObj = obj.get();

        // :: error: (argument.type.incompatible)
        new LostNonReflexive(obj.f);
        new LostNonReflexive(bottomObj);

        // :: error: (argument.type.incompatible)
        this.set(obj.f);
        this.set(bottomObj);
    }
}
