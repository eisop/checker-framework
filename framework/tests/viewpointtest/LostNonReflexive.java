import viewpointtest.quals.*;

@ReceiverDependentQual public class LostNonReflexive {
    @ReceiverDependentQual Object f;

    @ReceiverDependentQual LostNonReflexive(@ReceiverDependentQual Object args) {}

    @ReceiverDependentQual Object get(@Top LostNonReflexive this) {
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

        // :: error: (argument.type.incompatible)
        new LostNonReflexive(obj.f);
        new LostNonReflexive(bottomObj);

        // :: error: (argument.type.incompatible)
        this.set(obj.f);
        this.set(bottomObj);
    }
}
