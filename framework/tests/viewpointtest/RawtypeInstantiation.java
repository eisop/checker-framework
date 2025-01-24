// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
import viewpointtest.quals.*;

public class RawtypeInstantiation<C extends RawtypeInstantiation<C>> {

    void foo() {
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation rawtypeInstance = new @A RawtypeInstantiation();
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation<C> genericInstance = new @A RawtypeInstantiation<>();
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation<?> wildcardInstance = new @A RawtypeInstantiation<C>();
    }

    <D extends RawtypeInstantiation<D>> void bar(D b) {
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation rawtypeInstance = new @A RawtypeInstantiation();
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation<D> genericInstance = new @A RawtypeInstantiation<>();
        // :: warning: (cast.unsafe.constructor.invocation)
        RawtypeInstantiation<?> wildcardInstance = new @A RawtypeInstantiation<D>();
    }
}
