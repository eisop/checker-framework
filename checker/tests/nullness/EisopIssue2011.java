// Test case for https://github.com/eisop/checker-framework/issues/2011 :
// a cast to a type variable dropped the primary annotation of a type-variable expression.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

class EisopIssue2011<T extends @Nullable Object> {
    T launderViaLocal(@Nullable T p) {
        T x = (T) p;
        // :: error: (return.type.incompatible)
        return x;
    }

    T launderDirectly(@Nullable T p) {
        // :: error: (return.type.incompatible)
        return (T) p;
    }

    @NonNull T castNonNull(@NonNull T p) {
        return (T) p;
    }

    T castExplicitNonNull(@Nullable T p) {
        // :: warning: (cast.unsafe)
        return (@NonNull T) p;
    }

    <S extends T> T castToOtherTypeVar(@Nullable S p) {
        // :: error: (return.type.incompatible)
        return (T) p;
    }

    // A cast to the type variable does not assert that an element of a null-padded array is
    // non-null where the type argument is non-null (the pattern of plume-util's ArraySet).
    static class PaddedArray<E extends @Nullable Object> {
        @Nullable E[] values;

        PaddedArray(@Nullable E[] values) {
            this.values = values;
        }

        E get(int i) {
            E result = values[i];
            // :: error: (return.type.incompatible)
            return (E) result;
        }

        void forEach(java.util.function.Consumer<? super E> action, int i) {
            E e = values[i];
            // :: error: (argument.type.incompatible)
            action.accept((E) e);
        }

        @SuppressWarnings("nullness:return") // i is less than the size, so values[i] is an E
        E getSuppressed(int i) {
            return values[i];
        }
    }

    static void use() {
        EisopIssue2011<@NonNull String> b = new EisopIssue2011<>();
        @NonNull String s = b.launderViaLocal(null);
    }
}
