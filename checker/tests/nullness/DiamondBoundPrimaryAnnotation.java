// Test case for a diamond whose class has a type parameter bounded by an annotated use of another
// type parameter, `T extends @Nullable E`. After E is inferred, the bound must keep the @Nullable.
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

    final <S extends @Nullable T> DiamondBoundPrimaryAnnotation<Iterable<S>> pairwiseBad() {
        // :: error: (type.arguments.not.inferred)
        return new PairwiseNonNull<>(this);
    }
}
