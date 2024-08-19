// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
public class RawtypeInstantiation<A extends RawtypeInstantiation> {
    void foo() {
        RawtypeInstantiation rawtypeInstantiation = new RawtypeInstantiation();
    }
}
