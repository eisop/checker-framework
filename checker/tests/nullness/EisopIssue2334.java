// Test case for EISOP issue #2334:
// https://github.com/eisop/checker-framework/issues/2334
// Type argument inference crashed when a type variable bounded by another type variable was
// inferred from the assignment target: two upper bounds on the same inference variable, one a
// parameterization that mentions another inference variable and one a subclass of that
// parameterization, did not imply the equality of their type arguments (JLS 18.3.1).

import java.util.Collections;
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

    // Did not crash: the argument instantiates T directly. (The issue uses List.of, which Java 8
    // does not have.)
    StringBox f3 = box(Collections.singletonList("x"));

    void uses() {
        StringBox l1 = box();
        StringBox l2 = box(make());
        StringBox l3 = box(Collections.singletonList("x"));
        Box<String> b1 = box();
        Box<String> b2 = box(make());
    }

    // An F-bounded type variable inferred from an enum argument and an enum target: the target's
    // supertype Enum<Prefix> has the target's primary qualifier on its type argument, which must
    // not be forced onto T.
    enum Prefix {
        one,
        kilo
    }

    static <T extends Enum<T>> T getElementValueEnum(
            Object anno, Class<T> expectedType, T defaultValue) {
        return defaultValue;
    }

    static <T extends Enum<T>> T getElementValueEnum2(Object anno, Class<T> expectedType) {
        return Enum.valueOf(expectedType, "one");
    }

    static <T extends Enum<T>> T fromClass(Class<T> expectedType) {
        return Enum.valueOf(expectedType, "one");
    }

    void enums(Object anno) {
        Prefix p = getElementValueEnum(anno, Prefix.class, Prefix.one);
        Prefix q = getElementValueEnum2(anno, Prefix.class);
        Prefix r = fromClass(Prefix.class);
        Enum<?> s = getElementValueEnum(anno, Prefix.class, Prefix.one);
    }
}
