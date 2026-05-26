import viewpointtest.quals.*;

public class LostNonReflexive {
    @ReceiverDependentQual Object f;
    @ReceiverDependentQual LostNonReflexive f2;

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual LostNonReflexive(@ReceiverDependentQual Object args) {}

    @ReceiverDependentQual Object get() {
        return null;
    }

    @PolyVP LostNonReflexive identity(@PolyVP LostNonReflexive this) {
        return this;
    }

    void set(@ReceiverDependentQual Object o) {}

    void test(@Top LostNonReflexive obj, @Bottom Object bottomObj) {
        // :: error: (viewpointtest.lost.lhs)
        this.f = obj.f;
        // :: error: (viewpointtest.lost.lhs)
        this.f = bottomObj;

        // :: error: (assignment.type.incompatible)
        @A Object aObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @B Object bObj = obj.get();
        // :: error: (assignment.type.incompatible)
        @Bottom Object botObj = obj.get();

        // :: error: (new.class.type.invalid)
        new LostNonReflexive(obj.f);
        // :: error: (new.class.type.invalid)
        new LostNonReflexive(bottomObj);

        this.set(obj.f);
        this.set(bottomObj);

        obj.f2.identity();
    }
}
