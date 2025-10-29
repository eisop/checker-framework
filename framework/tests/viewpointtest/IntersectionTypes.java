// Test case for EISOP Issue 433:
// https://github.com/eisop/checker-framework/issues/433

import viewpointtest.quals.*;

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

public class IntersectionTypes {
    void foo() {
        // :: warning: (cast.unsafe.constructor.invocation)
        Baz baz = new @A Baz();
        call(baz);
    }

    <T extends Foo & Bar> void call(T p) {}
}
