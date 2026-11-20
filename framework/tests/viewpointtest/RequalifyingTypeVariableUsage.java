import viewpointtest.quals.*;

@SuppressWarnings("cast.unsafe.constructor.invocation")
class RequalifyingTypeVariableUsage {
    static class Element {}

    static class Fields<E extends @Top Object> {
        @ReceiverDependentQual E receiverDependent;
        @A E requalifyingA;
        E bare;
    }

    void fieldTypeVariableUsages() {
        @A Fields<@B Element> fields = new @A Fields<>();

        fields.receiverDependent = new @A Element();
        // :: error: (assignment.type.incompatible)
        fields.receiverDependent = new @B Element();

        fields.requalifyingA = new @A Element();
        // :: error: (assignment.type.incompatible)
        fields.requalifyingA = new @B Element();

        fields.bare = new @B Element();
        // :: error: (assignment.type.incompatible)
        fields.bare = new @A Element();
    }

    static class Methods<E extends @Top Object> {
        @ReceiverDependentQual E receiverDependent() {
            return null;
        }

        @A E requalifyingA() {
            return null;
        }

        E bare() {
            return null;
        }
    }

    void methodReturnTypeVariableUsages(@A Methods<@B Object> methods) {
        @A Object receiverDependent = methods.receiverDependent();
        @A Object requalifyingA = methods.requalifyingA();
        @B Object bare = methods.bare();
    }

    static class Captured<E> {
        // The type argument is a capture of "? extends E": a type variable, which has no head
        // qualifier for the requalifying usage "@A E" to replace.
        void requalifyingUsageOfCapturedTypeArgument(Captured<? extends E> captured) {
            captured.get();
        }

        Object get(Captured<@A E> this) {
            return null;
        }
    }
}
