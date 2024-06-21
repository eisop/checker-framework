// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
public class Generics<A extends Generics<A>> {
    void foo() {
        Generics generics = new Generics();
    }
}
