// Test case for eisop issue 2330:
// https://github.com/eisop/checker-framework/issues/2330

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

@SuppressWarnings("unchecked") // the casts to a type variable are unchecked
class EisopIssue2330Tainting<T> {
    T f(@Tainted Object o) {
        // :: error: (return.type.incompatible)
        return (T) o;
    }

    T g(T t) {
        return (T) t;
    }

    T h(@Untainted Object o) {
        return (T) o;
    }

    @Tainted T k(@Tainted Object o) {
        return (T) o;
    }
}
