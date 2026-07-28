// Test case for EISOP issue #1819:
// https://github.com/eisop/checker-framework/issues/1819

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

abstract class Issue1819<E> {
    <T extends @Nullable Object> void nullableTypeVarWildcard(Issue1819<? extends T> elements) {
        @Nullable Object nullable = elements.getObjectPoly();
        // :: error: (assignment.type.incompatible)
        @NonNull Object nonnull = elements.getObjectPoly();
    }

    abstract @PolyNull Object getObjectPoly(Issue1819<@PolyNull E> this);

    abstract static class Recursive<E extends @Nullable Recursive<E>> {
        <T extends @Nullable Recursive<T>> void nullableRecursiveTypeVarWildcard(
                Recursive<? extends T> elements) {
            @Nullable Object nullable = elements.getObjectPoly();
            // :: error: (assignment.type.incompatible)
            @NonNull Object nonnull = elements.getObjectPoly();
        }

        abstract @PolyNull Object getObjectPoly(Recursive<@PolyNull E> this);
    }

    abstract static class Chained<E> {
        <F extends @Nullable Object, T extends F, U extends T> void nullableChainedTypeVarWildcard(
                Chained<? extends U> elements) {
            @Nullable Object nullable = elements.getObjectPoly();
            // :: error: (assignment.type.incompatible)
            @NonNull Object nonnull = elements.getObjectPoly();
        }

        abstract @PolyNull Object getObjectPoly(Chained<@PolyNull E> this);
    }
}
