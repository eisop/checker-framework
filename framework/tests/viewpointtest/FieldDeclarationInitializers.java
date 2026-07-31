import viewpointtest.quals.A;
import viewpointtest.quals.B;
import viewpointtest.quals.ReceiverDependentQual;

@SuppressWarnings({
    "inconsistent.constructor.type",
    "super.invocation.invalid",
    "cast.unsafe.constructor.invocation"
})
@A class FieldDeclarationInitializers {
    @ReceiverDependentQual Object compatible = new @A Object();

    // :: error: (assignment.type.incompatible)
    @ReceiverDependentQual Object incompatible = new @B Object();

    @ReceiverDependentQual GenericBox<@ReceiverDependentQual Object> genericCompatible = new @A GenericBox<@A Object>();

    @ReceiverDependentQual GenericBox<@ReceiverDependentQual Object> genericOuterCompatible =
            // :: error: (assignment.type.incompatible)
            new @A GenericBox<@B Object>();

    @ReceiverDependentQual GenericBox<@ReceiverDependentQual Object> genericArgumentCompatible =
            // :: error: (assignment.type.incompatible)
            new @B GenericBox<@A Object>();

    // :: error: (assignment.type.incompatible)
    @ReceiverDependentQual GenericBox<@ReceiverDependentQual Object> genericIncompatible = new @B GenericBox<@B Object>();

    @B Object fixed = new @B Object();

    // Static fields have no receiver viewpoint.
    static @B Object staticField = new @B Object();

    static class GenericBox<T> {}
}
