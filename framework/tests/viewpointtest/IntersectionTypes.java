// Test case for EISOP Issue 433:
// https://github.com/eisop/checker-framework/issues/433

interface Foo {}

interface Bar {}

class Baz implements Foo, Bar {}

public class IntersectionTypes {
    void foo() {
        Baz baz = new Baz();
        call(baz);
    }

    <T extends Foo & Bar> void call(T p) {}
}
