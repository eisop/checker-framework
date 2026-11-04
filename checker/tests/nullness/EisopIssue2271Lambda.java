// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271
// An anonymous class created with a diamond in a lambda body, and one whose superclass constructor
// has a polymorphic qualifier, get the inferred superclass type arguments.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

class EisopIssue2271Lambda {
    static class Cell<T extends @Nullable Object> {
        Cell(T t) {}

        T get() {
            throw new RuntimeException();
        }
    }

    interface Make {
        Cell<@Nullable Object> make();
    }

    void inLambda(@Nullable Object n) {
        Make m = () -> new Cell<>(n) {};
        Make bad =
                () -> {
                    // :: error: (type.arguments.not.inferred)
                    Cell<@NonNull Object> c = new Cell<>(n) {};
                    // :: error: (return.type.incompatible)
                    return c;
                };
    }

    static class PolyCell<T extends @Nullable Object> {
        PolyCell(@PolyNull Object o) {}
    }

    void poly(@Nullable Object n, @NonNull Object nn) {
        PolyCell<@Nullable Object> a = new PolyCell<>(n) {};
        PolyCell<@Nullable Object> b = new PolyCell<>(nn) {};
    }
}
