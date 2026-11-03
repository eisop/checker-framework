// Test case for a type parameter bounded by an annotated use of another type parameter,
// `T extends @Nullable E`, of a class instantiated with a diamond and of a generic method. After E
// is inferred, the bound must keep the @Nullable.
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class DiamondBoundPrimaryAnnotation<T> {
    static final class Pairwise<E, T extends @Nullable E>
            extends DiamondBoundPrimaryAnnotation<Iterable<T>> {
        Pairwise(DiamondBoundPrimaryAnnotation<E> elementEquivalence) {}
    }

    static final class PairwiseNonNull<E, T extends E>
            extends DiamondBoundPrimaryAnnotation<Iterable<T>> {
        PairwiseNonNull(DiamondBoundPrimaryAnnotation<E> elementEquivalence) {}
    }

    final <S extends @Nullable T> DiamondBoundPrimaryAnnotation<Iterable<S>> pairwise() {
        return new Pairwise<>(this);
    }

    final <S extends @Nullable T> DiamondBoundPrimaryAnnotation<Iterable<S>> pairwiseExplicit() {
        return new Pairwise<T, S>(this);
    }

    final <S extends T> DiamondBoundPrimaryAnnotation<Iterable<S>> pairwiseNonNull() {
        return new PairwiseNonNull<>(this);
    }

    static <E, U extends @Nullable E> Iterable<U> pairwiseOf(
            DiamondBoundPrimaryAnnotation<E> elementEquivalence) {
        throw new Error();
    }

    static <E, U extends E> Iterable<U> pairwiseOfNonNull(
            DiamondBoundPrimaryAnnotation<E> elementEquivalence) {
        throw new Error();
    }

    final <S extends @Nullable T> Iterable<S> pairwiseMethod() {
        return pairwiseOf(this);
    }

    final <S extends @Nullable T> Iterable<S> pairwiseMethodBad() {
        // :: error: (type.arguments.not.inferred)
        return pairwiseOfNonNull(this);
    }

    final <S extends @Nullable T> DiamondBoundPrimaryAnnotation<Iterable<S>> pairwiseBad() {
        // :: error: (type.arguments.not.inferred)
        return new PairwiseNonNull<>(this);
    }
}
