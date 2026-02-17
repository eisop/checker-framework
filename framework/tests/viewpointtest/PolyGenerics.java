import viewpointtest.quals.*;

// Test case for EISOP issue #1074:
// https://github.com/eisop/checker-framework/issues/1074
public class PolyGenerics<E, F> {
    void copyOf(PolyGenerics<? extends E, ? extends F> elements) {
        Object[] array = elements.toArray();
        elements.getFirstElement();
        elements.getSecondElement();
    }

    @PolyVP Object[] toArray(PolyGenerics<@PolyVP E, @PolyVP F> this) {
        return null;
    }

    @PolyVP E getFirstElement(PolyGenerics<@PolyVP E, F> this) {
        return null;
    }

    @PolyVP F getSecondElement(PolyGenerics<E, @PolyVP F> this) {
        return null;
    }
}
