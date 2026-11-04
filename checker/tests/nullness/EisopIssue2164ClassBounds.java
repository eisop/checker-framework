// Test case for https://github.com/eisop/checker-framework/issues/2164
// Nested calls whose type parameters have class bounds, such as Number and Enum<E>: the glb of
// such a bound with the bound by the other inference variable is not a type, and inference used
// to crash with a FalseBoundException.  The false positives are
// https://github.com/eisop/checker-framework/issues/2321; the test only checks that inference does
// not crash.

import java.util.Collections;

public class EisopIssue2164ClassBounds {
    static <E extends Enum<E>> E castEnum(Object o) {
        throw new Error();
    }

    static <T extends Number & Comparable<? super T>> T castNumber(Object o) {
        throw new Error();
    }

    static <U> U id(U u) {
        throw new Error();
    }

    static <U extends java.io.Serializable> U serializable(U u) {
        throw new Error();
    }

    void classBounds(Object o) {
        @SuppressWarnings("argument.type.incompatible") // eisop#2321
        Object r1 = id(castEnum(o));
        @SuppressWarnings("argument.type.incompatible") // eisop#2321
        Object r2 = id(castNumber(o));
        @SuppressWarnings("argument.type.incompatible") // eisop#2321
        Object r3 = Collections.singletonList(castNumber(o));
        @SuppressWarnings("argument.type.incompatible") // eisop#2321
        Object r4 = serializable(castNumber(o));
    }
}
