import viewpointtest.quals.*;

// Test case for EISOP issue #1074:
// https://github.com/eisop/checker-framework/issues/1074
public class PolyGenerics<E> {
    static <E> Object copyOf(PolyGenerics<? extends E> elements) {
        Object[] array = elements.toArray();
        return null;
    }

    Object[] toArray(PolyGenerics<@PolyVP E> this) {
        return null;
    }
}
