// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2271
// A diamond on an anonymous subclass of an inner class.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2271Enclosing<U> {
    class Inner<T> {
        T t;

        Inner(T t) {
            this.t = t;
        }

        U u() {
            throw new RuntimeException();
        }
    }

    static class Nested<T> {
        class Deep<V> {
            Deep(V v) {}

            V get() {
                throw new RuntimeException();
            }

            T outer() {
                throw new RuntimeException();
            }
        }
    }

    void unqualified(@Nullable Object n) {
        Inner<@Nullable Object> a = new Inner<>(n) {};
        // :: error: (type.arguments.not.inferred)
        Inner<Object> b = new Inner<>(n) {};
        // :: error: (dereference.of.nullable)
        new Inner<>(n) {}.t.toString();
    }

    static void qualified(EisopIssue2271Enclosing<@Nullable String> o, @Nullable Object n) {
        EisopIssue2271Enclosing<@Nullable String>.Inner<@Nullable Object> a = o.new Inner<>(n) {};
        // :: error: (type.arguments.not.inferred)
        EisopIssue2271Enclosing<@Nullable String>.Inner<Object> b = o.new Inner<>(n) {};
        // :: error: (dereference.of.nullable)
        o.new Inner<>(n) {}.t.toString();
        // :: error: (dereference.of.nullable)
        o.new Inner<>("") {}.u().toString();
        o.new Inner<>("") {}.t.toString();
    }

    static void deep(Nested<@Nullable String> o, @Nullable Object n) {
        Nested<@Nullable String>.Deep<@Nullable Object> a = o.new Deep<>(n) {};
        // :: error: (dereference.of.nullable)
        o.new Deep<>(n) {}.get().toString();
        // :: error: (dereference.of.nullable)
        o.new Deep<>("") {}.outer().toString();
        o.new Deep<>("") {}.get().toString();
    }
}
