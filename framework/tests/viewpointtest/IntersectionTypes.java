// Test case for EISOP Issue 433:
// https://github.com/eisop/checker-framework/issues/433

import viewpointtest.quals.*;

public class IntersectionTypes {

    interface Foo {}

    interface Bar {}

    class Baz implements Foo, Bar {}

    <T extends Foo & Bar> void call(T p) {}

    void foo() {
        // :: warning: (cast.unsafe.constructor.invocation)
        Baz baz = new @A Baz();
        call(baz);
    }

    interface B<X> {}

    interface C<X> {}

    abstract class D<X extends B<X> & C<X>> {}

    class BC implements B<BC>, C<BC> {}

    class E extends D<BC> {}

    <T extends B<T> & C<T>> void call(T p) {}
}
