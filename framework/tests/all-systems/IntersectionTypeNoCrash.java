// Test case for EISOP Issue 433:
// https://github.com/eisop/checker-framework/issues/433

public class IntersectionTypeNoCrash {

    interface Foo {}

    interface Bar {}

    class Baz implements Foo, Bar {}

    <T extends Foo & Bar> void call(T p) {}

    void test() {
        call(new Baz());
    }

    void testCast(Object obj) {
        Foo fooAndBar = (Foo & Bar) obj;
    }
}
