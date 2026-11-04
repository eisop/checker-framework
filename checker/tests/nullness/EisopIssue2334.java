// Test case for EISOP issue #2334:
// https://github.com/eisop/checker-framework/issues/2334
// Type argument inference crashed when a type variable whose bound mentions another type variable
// was inferred from the assignment target: two upper bounds on the same inference variable, one a
// parameterization that mentions another inference variable and one a subclass of that
// parameterization, did not imply the equality of their type arguments (JLS 18.3.1).

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.List;

class EisopIssue2334 {
    static class Box<T> {}

    static class StringBox extends Box<String> {}

    static class NullableStringBox extends Box<@Nullable String> {}

    static <T, B extends Box<T>> B box() {
        throw new Error();
    }

    static <T, B extends Box<T>> B box(List<T> l) {
        throw new Error();
    }

    static <T, B extends Box<T>> B boxOf(T t) {
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
    StringBox f3 = box(Collections.singletonList("x"));

    void uses() {
        StringBox l1 = box();
        StringBox l2 = box(make());
        StringBox l3 = box(Collections.singletonList("x"));
        Box<String> b1 = box();
        Box<String> b2 = box(make());
    }

    // The target implies T = String, including its qualifier.
    void qualifiers() {
        // :: error: (type.arguments.not.inferred)
        StringBox s = boxOf(null);
        NullableStringBox n = boxOf(null);
        StringBox t = boxOf("x");
    }

    // The type argument of the supertype Enum<Prefix> of an enum type has the primary qualifier of
    // the enum type, which must not be forced onto T: a @Nullable target does not make T nullable.
    // framework/tests/all-systems/EnumInference.java has the cases without qualifiers.
    enum Prefix {
        one,
        kilo
    }

    static <T extends Enum<T>> T getElementValueEnum(
            Object anno, Class<T> expectedType, T defaultValue) {
        return defaultValue;
    }

    static <T extends Enum<T>> T fromClass(Class<T> expectedType) {
        return Enum.valueOf(expectedType, "one");
    }

    void enums(Object anno) {
        @Nullable Prefix p = getElementValueEnum(anno, Prefix.class, Prefix.one);
        @Nullable Prefix r = fromClass(Prefix.class);
        // :: error: (type.arguments.not.inferred)
        Prefix q = getElementValueEnum(anno, Prefix.class, null);
    }

    <E extends Enum<E>> void typeVariable(Class<E> c, E e, Object anno) {
        E x = getElementValueEnum(anno, c, e);
        @Nullable E y = getElementValueEnum(anno, c, e);
    }
}
