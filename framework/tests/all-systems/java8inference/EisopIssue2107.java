// Test case for https://github.com/eisop/checker-framework/issues/2107
// A call to a generic method whose return type is a wildcard parameterization of a class with
// F-bounded type parameters, where one type parameter is bounded by another.

public class EisopIssue2107 {
    static class Pair<X, Y extends X> {}

    static <T> Pair<? extends T, ? extends T> make() {
        throw new Error();
    }

    void test() {
        Pair<?, ?> p = make();
    }
}
