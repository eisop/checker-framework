// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
import viewpointtest.quals.*;

public class SelfReferentialType<C extends SelfReferentialType<C>> {
    // :: error: (super.invocation.invalid) :: warning: (inconsistent.constructor.type)
    public @A SelfReferentialType() {}

    void createInstances() {
        SelfReferentialType rawtypeInstance = new @A SelfReferentialType();
        SelfReferentialType<C> genericInstance = new @A SelfReferentialType<>();
        SelfReferentialType<?> wildcardInstance = new @A SelfReferentialType<C>();
    }

    <D extends SelfReferentialType<D>> void createInstancesUsingMethodTypeParameter() {
        SelfReferentialType rawtypeInstance = new @A SelfReferentialType();
        SelfReferentialType<D> genericInstance = new @A SelfReferentialType<>();
        SelfReferentialType<?> wildcardInstance = new @A SelfReferentialType<D>();
    }
}
