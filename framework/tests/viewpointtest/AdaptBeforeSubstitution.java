// Viewpoint adaptation adapts the declared type of a member from the receiver's viewpoint and
// then substitutes the receiver's type arguments for the class's type variables. The type
// arguments are already relative to the current viewpoint, so they are not adapted: a member of
// type X accessed through `@A Box<@ReceiverDependentQual Object>` has type
// `@ReceiverDependentQual Object`, not `@A Object`. Substituting first and adapting afterwards
// would adapt the type argument and give `@A Object`; each case below then reports the opposite
// set of errors.

import viewpointtest.quals.*;

public class AdaptBeforeSubstitution {

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    static class Box<X> {
        X value;

        @ReceiverDependentQual Box(X value) {
            this.value = value;
        }

        X get() {
            return value;
        }

        void set(X x) {
            value = x;
        }
    }

    void invoke(
            @A Box<@ReceiverDependentQual Object> box,
            @ReceiverDependentQual Object rd,
            @A Object a) {
        @ReceiverDependentQual Object r = box.get();
        // :: error: (assignment.type.incompatible)
        @A Object wrong = box.get();
        box.set(rd);
        // :: error: (argument.type.incompatible)
        box.set(a);
    }

    void field(
            @A Box<@ReceiverDependentQual Object> box,
            @ReceiverDependentQual Object rd,
            @A Object a) {
        @ReceiverDependentQual Object r = box.value;
        // :: error: (assignment.type.incompatible)
        @A Object wrong = box.value;
        box.value = rd;
        // :: error: (assignment.type.incompatible)
        box.value = a;
    }

    void construct(@ReceiverDependentQual Object rd, @A Object a) {
        new @A Box<@ReceiverDependentQual Object>(rd);
        // :: error: (argument.type.incompatible)
        new @A Box<@ReceiverDependentQual Object>(a);
    }
}
