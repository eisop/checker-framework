import viewpointtest.quals.*;

// Test case for EISOP issue #778:
// https://github.com/eisop/checker-framework/issues/778
//
// A raw use of an F-bounded class has a cyclic type graph: the raw type's implicit wildcard bound
// leads back to the same declared type. Viewpoint adaptation must copy such a graph rather than
// recurse into it forever. Each member below reaches the cycle by a different route.
@SuppressWarnings("rawtypes")
public class RawFBoundedTypes {
    static class Rec<T extends Rec<T>> {}

    static class Plain<T> {}

    interface Marker {}

    // Through the extends clause, via postDirectSuperTypes.
    static class RawSupertype extends Rec {}

    // Through a field type, with no extends clause and no supertype computation at all.
    Rec field;

    // Through a type-parameter bound, which BoundsInitializer builds rather than
    // AnnotatedTypeMirror#getTypeArguments.
    static class RawBound<E extends Rec> {
        @ReceiverDependentQual E e;

        <T extends E> T pick() {
            return null;
        }
    }

    // Through an intersection bound.
    static class RawIntersectionBound<E extends Rec & Marker> {
        @ReceiverDependentQual E e;
    }

    // Negative controls: none of these is cyclic, and all must stay clean.

    // Not raw: the type argument is a distinct class, so the graph is finite.
    static class NonRawSupertype extends Rec<NonRawSupertype> {}

    // Raw but not F-bounded: the implicit wildcard bound does not lead back to Plain.
    Plain rawNotFBounded;

    // A wildcard type argument is not the same as a raw type.
    Rec<?> wildcardArgument;
}
