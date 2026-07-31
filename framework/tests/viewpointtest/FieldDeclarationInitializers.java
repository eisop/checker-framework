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

    @B Object fixed = new @B Object();

    // Static fields have no receiver viewpoint.
    static @B Object staticField = new @B Object();
}
