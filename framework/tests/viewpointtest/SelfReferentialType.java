// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
import viewpointtest.quals.*;

public class SelfReferentialType<C extends SelfReferentialType<C>> {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @ReceiverDependentQual SelfReferentialType() {}

    void createInstances() {
        @A SelfReferentialType rawtypeInstance = new @A SelfReferentialType();
        @A SelfReferentialType<C> aGenericInstance = new @A SelfReferentialType<>();
        @B SelfReferentialType<C> bGenericInstance = new @B SelfReferentialType<>();
        @A SelfReferentialType<?> wildcardInstance = new @A SelfReferentialType<C>();
        // :: error: (assignment.type.incompatible)
        @B SelfReferentialType<C> incorrectlyAdaptedToB = new @A SelfReferentialType<>();
        // :: error: (assignment.type.incompatible)
        @A SelfReferentialType<C> incorrectlyAdaptedToA = new @B SelfReferentialType<>();
    }

    <D extends SelfReferentialType<D>> void createInstancesUsingMethodTypeParameter() {
        @A SelfReferentialType rawtypeInstance = new @A SelfReferentialType();
        @A SelfReferentialType<D> aGenericInstance = new @A SelfReferentialType<>();
        @B SelfReferentialType<D> bGenericInstance = new @B SelfReferentialType<>();
        @A SelfReferentialType<?> wildcardInstance = new @A SelfReferentialType<D>();
        // :: error: (assignment.type.incompatible)
        @B SelfReferentialType<D> incorrectlyAdaptedToB = new @A SelfReferentialType<>();
        // :: error: (assignment.type.incompatible)
        @A SelfReferentialType<D> incorrectlyAdaptedToA = new @B SelfReferentialType<>();
    }
}
