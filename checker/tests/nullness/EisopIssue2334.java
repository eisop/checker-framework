// Test case for EISOP issue #2334:
// https://github.com/eisop/checker-framework/issues/2334
// Type argument inference crashed when a type variable bounded by another type variable was
// inferred from the assignment target: two upper bounds on the same inference variable, one a
// parameterization that mentions another inference variable and one a subclass of that
// parameterization, did not imply the equality of their type arguments (JLS 18.3.1).

import java.util.List;

class EisopIssue2334 {
    static class Box<T> {}

    static class StringBox extends Box<String> {}

    static <T, B extends Box<T>> B box() {
        throw new Error();
    }

    static <T, B extends Box<T>> B box(List<T> l) {
        throw new Error();
    }

    static <T> List<T> make() {
        throw new Error();
    }

    // Crashed: T is only determined by the target through the bound of B.
    StringBox f1 = box();

    // Crashed: the argument constrains T only against another inference variable.
    StringBox f2 = box(make());

    // Did not crash: the argument instantiates T directly.
    StringBox f3 = box(List.of("x"));

    void uses() {
        StringBox l1 = box();
        StringBox l2 = box(make());
        StringBox l3 = box(List.of("x"));
        Box<String> b1 = box();
        Box<String> b2 = box(make());
    }
}
