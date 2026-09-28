import viewpointtest.quals.*;

// Test case for EISOP issue #1074:
// https://github.com/eisop/checker-framework/issues/1074
public class PolyGenerics<E, F> {
    void copyOf(PolyGenerics<? extends E, ? extends F> elements) {
        Object[] array = elements.toArray();
        elements.getFirstElement();
        elements.getSecondElement();
    }

    void concreteQualifiers(PolyGenerics<? extends @A Object, ? extends @B Object> elements) {
        @A Object first = elements.getFirstObject();
        @B Object second = elements.getSecondObject();

        // :: error: (assignment.type.incompatible)
        @B Object badFirst = elements.getFirstObject();
        // :: error: (assignment.type.incompatible)
        @A Object badSecond = elements.getSecondObject();
    }

    void lowerBoundQualifiers(PolyGenerics<? super @A String, ? super @B String> elements) {
        @Top Object first = elements.getFirstObject();
        @Top Object second = elements.getSecondObject();

        // :: error: (assignment.type.incompatible)
        @A Object badFirst = elements.getFirstObject();
        // :: error: (assignment.type.incompatible)
        @B Object badSecond = elements.getSecondObject();
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

    @PolyVP Object getFirstObject(PolyGenerics<@PolyVP E, F> this) {
        return null;
    }

    @PolyVP Object getSecondObject(PolyGenerics<E, @PolyVP F> this) {
        return null;
    }
}
