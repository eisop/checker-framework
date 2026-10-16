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

    static void use() {
        EisopIssue2011<@NonNull String> b = new EisopIssue2011<>();
        @NonNull String s = b.launderViaLocal(null);
    }
}
