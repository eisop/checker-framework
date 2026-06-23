import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

import viewpointtest.quals.*;

public class RequalifyingTypeVar {
    abstract static class Box<E extends @Top Object> {
        abstract E bare();

        abstract @A E concreteA();

        abstract @ReceiverDependentQual E receiverDependent();
    }

    void bareTypeVariableUse(@Top Box<@B Object> box) {
        @B Object bare = box.bare();
        // :: error: (assignment.type.incompatible)
        @A Object badBare = box.bare();
    }

    void concreteTypeVariableUse(@Top Box<@B Object> box) {
        @A Object concreteA = box.concreteA();
        // :: error: (assignment.type.incompatible)
        @B Object badConcreteA = box.concreteA();
    }

    void viewpointAdaptedConcrete(@A Box<@B Object> a, @Top Box<@Bottom Object> top) {
        @A Object receiverDependentA = a.receiverDependent();
        // :: error: (assignment.type.incompatible)
        @B Object badReceiverDependentA = a.receiverDependent();

        // @Top viewpoint-adapts @ReceiverDependentQual to @Lost.
        // :: error: (assignment.type.incompatible)
        @Lost Object lost = top.receiverDependent();
    }

    @DefaultQualifier(
            value = A.class,
            locations = {TypeUseLocation.TYPE_VARIABLE_USE})
    abstract static class DefaultABox<E extends @Top Object> {
        abstract E bare();
    }

    void defaultedTypeVarUse(@Top DefaultABox<@B Object> box) {
        @B Object bare = box.bare();
        // :: error: (assignment.type.incompatible)
        @A Object badBare = box.bare();
    }
}
