// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271
// A diamond on an anonymous class must take the type-argument qualifiers from the target type, as
// a diamond on a named class does.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2271 {
    static class Box<T> {
        T get() {
            throw new RuntimeException();
        }

        void set(T t) {}
    }

    Box<@Nullable Object> anonDiamond = new Box<>() {};
    Box<@Nullable Object> anonExplicit = new Box<@Nullable Object>() {};
    Box<@Nullable Object> namedDiamond = new Box<>();

    Box<Object> nonNullAnonDiamond = new Box<>() {};

    Box<@Nullable Object> overrides =
            new Box<>() {
                @Override
                void set(@Nullable Object o) {}
            };

    // :: error: (assignment.type.incompatible)
    Box<Object> wrong = new Box<@Nullable Object>() {};

    static void take(Box<@Nullable String> b) {}

    static void arguments() {
        take(new Box<>() {});
    }

    static Box<@Nullable String> returns() {
        return new Box<>() {};
    }

    static void inferredFromTarget() {
        Box<@Nullable String> b = new Box<>() {};
        b.set(null);
        // :: error: (dereference.of.nullable)
        b.get().length();
    }

    static class Cell<T> {
        T t;

        Cell(T t) {
            this.t = t;
        }
    }

    static <S> S unwrap(Cell<S> c) {
        return c.t;
    }

    static void constructorArguments(@Nullable Object n) {
        Cell<@Nullable Object> named = new Cell<>(n);
        Cell<@Nullable Object> anon = new Cell<>(n) {};
        // :: error: (dereference.of.nullable)
        new Cell<>(n) {}.t.toString();
        // :: error: (dereference.of.nullable)
        unwrap(new Cell<>(n) {}).toString();
    }
}
