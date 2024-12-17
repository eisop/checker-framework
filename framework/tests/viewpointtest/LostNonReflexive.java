import viewpointtest.quals.*;

public class LostNonReflexive {
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

        // :: error: (assignment.type.incompatible)
        @A Object aObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @B Object bObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @Bottom Object botObj = obj.get();

        // :: error: (argument.type.incompatible) :: error: (new.class.type.invalid)
        new LostNonReflexive(obj.f);
        // :: error: (new.class.type.invalid)
        new LostNonReflexive(bottomObj);

        // :: error: (argument.type.incompatible)
        this.set(obj.f);
        this.set(bottomObj);
    }
}
