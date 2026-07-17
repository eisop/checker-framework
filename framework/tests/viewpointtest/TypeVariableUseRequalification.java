import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class TypeVariableUseRequalification {
    static class Element {}

    static class Fields<E extends @Top Object> {
        @ReceiverDependentQual E receiverDependent;
        @A E concreteA;
        E bare;
    }

    void fieldTypeVariableUses() {
        @A Fields<@B Element> fields = new @A Fields<>();

        fields.receiverDependent = new @A Element();
        // :: error: (assignment.type.incompatible)
        fields.receiverDependent = new @B Element();

        fields.concreteA = new @A Element();
        // :: error: (assignment.type.incompatible)
        fields.concreteA = new @B Element();

        fields.bare = new @B Element();
        // :: error: (assignment.type.incompatible)
        fields.bare = new @A Element();
    }

    static class Methods<E extends @Top Object> {
        @ReceiverDependentQual E receiverDependent() {
            return null;
        }

        @A E concreteA() {
            return null;
        }

        E bare() {
            return null;
        }
    }

    void methodReturnTypeVariableUses(@A Methods<@B Object> methods) {
        @A Object receiverDependent = methods.receiverDependent();
        @A Object concreteA = methods.concreteA();
        @B Object bare = methods.bare();
    }
}
