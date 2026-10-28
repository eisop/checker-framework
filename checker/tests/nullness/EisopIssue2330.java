// Test case for eisop issue 2330:
// https://github.com/eisop/checker-framework/issues/2330

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unchecked") // the casts to a type variable are unchecked
class EisopIssue2330<T> {
    T f(@Nullable Object o) {
        // :: error: (return.type.incompatible)
        return (T) o;
    }

    T g(T t) {
        return (T) t;
    }

    T h(@NonNull Object o) {
        return (T) o;
    }

    static <U> U i(@Nullable Object o) {
        // :: error: (return.type.incompatible)
        return (U) o;
    }

    T j(@Nullable Object o) {
        // The local variable is refined to the type of the cast, @Nullable T.
        T x = (T) o;
        // :: error: (return.type.incompatible)
        return x;
    }

    @Nullable T k(@Nullable Object o) {
        return (T) o;
    }

    static void use() {
        new EisopIssue2330<@NonNull String>().f(null).length();
    }
}
