// @below-java11-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271
// The type arguments of the superclass of an anonymous class created with a diamond are inferred
// as for a diamond on the superclass, and the arguments are checked against them.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class EisopIssue2271Arguments {
    static class Cell<T> {
        T t;

        Cell(T t) {
            this.t = t;
        }

        T get() {
            return t;
        }

        void set(T t) {
            this.t = t;
        }
    }

    interface Fn<T> {
        T apply();
    }

    static class VCell<T> {
        @SafeVarargs
        VCell(T... ts) {}

        T get() {
            throw new RuntimeException();
        }
    }

    static class GCell<T> {
        <U> GCell(U u, T t) {}
    }

    static <S> S unwrap(Cell<S> c) {
        return c.t;
    }

    static void takeNullable(Cell<@Nullable Object> c) {}

    static void takeNonNull(Cell<Object> c) {}

    static void arguments(@Nullable Object n, Object nn) {
        // :: error: (type.arguments.not.inferred)
        Cell<Object> named = new Cell<>(n);
        // :: error: (type.arguments.not.inferred)
        Cell<Object> anon = new Cell<>(n) {};
        Cell<Object> nonNullNamed = new Cell<>(nn);
        Cell<Object> nonNullAnon = new Cell<>(nn) {};
        Cell<@Nullable Object> nullableFromNonNull = new Cell<>(nn) {};
        nullableFromNonNull.set(null);
        // :: error: (type.arguments.not.inferred)
        takeNonNull(new Cell<>(n) {});
        takeNullable(new Cell<>(n) {});
        takeNullable(new Cell<>(nn) {});
        // :: error: (dereference.of.nullable)
        unwrap(new Cell<>(n) {}).toString();
        unwrap(new Cell<>(nn) {}).toString();
    }

    static void noTarget(Object nn, @Nullable Object n) {
        var x = new Cell<>(nn) {};
        // :: error: (argument.type.incompatible)
        x.set(null);
        var y = new Cell<>(nn);
        // :: error: (argument.type.incompatible)
        y.set(null);
        var z = new Cell<>(n) {};
        z.set(null);
        // :: error: (dereference.of.nullable)
        z.t.toString();
        // :: error: (dereference.of.nullable)
        new Cell<>(n) {}.get().toString();
        new Cell<>(nn) {}.get().toString();
    }

    static void body(@Nullable Object n, Object nn) {
        new Cell<>(n) {
            private void m() {
                // :: error: (dereference.of.nullable)
                t.toString();
            }
        };
        new Cell<>(nn) {
            private void m() {
                t.toString();
            }
        };
    }

    static void overrides() {
        Cell<@Nullable Object> c =
                new Cell<>(null) {
                    @Override
                    // :: error: (override.param.invalid)
                    void set(Object o) {}
                };
        Cell<Object> d =
                new Cell<>("") {
                    @Override
                    // :: error: (override.return.invalid)
                    @Nullable Object get() {
                        return null;
                    }
                };
        Cell<Object> e =
                new Cell<>("") {
                    @Override
                    void set(@Nullable Object o) {}
                };
    }

    static void interfaces() {
        Fn<@Nullable String> f =
                new Fn<>() {
                    @Override
                    public @Nullable String apply() {
                        return null;
                    }
                };
        Fn<String> g =
                new Fn<>() {
                    @Override
                    // :: error: (override.return.invalid)
                    public @Nullable String apply() {
                        return null;
                    }
                };
    }

    static void varargs(@Nullable Object n) {
        VCell<@Nullable Object> v = new VCell<>(n, n) {};
        // :: error: (type.arguments.not.inferred)
        VCell<Object> w = new VCell<>(n, "") {};
        // :: error: (dereference.of.nullable)
        new VCell<>(n, n) {}.get().toString();
    }

    static void genericConstructor(@Nullable Object n) {
        GCell<@Nullable Object> g = new GCell<>("", n) {};
        // :: error: (type.arguments.not.inferred)
        GCell<Object> h = new GCell<>("", n) {};
    }

    static Cell<@Nullable Object> returns(@Nullable Object n) {
        return new Cell<>(n) {};
    }

    static Cell<Object> returnsNonNull(@Nullable Object n) {
        // :: error: (type.arguments.not.inferred)
        return new Cell<>(n) {};
    }

    static void conditional(boolean b, @Nullable Object n) {
        Cell<@Nullable Object> c = b ? new Cell<>(n) {} : new Cell<>(null) {};
    }

    static void nested(@Nullable Object n) {
        List<Cell<@Nullable Object>> l = List.of(new Cell<>(n) {});
    }
}
