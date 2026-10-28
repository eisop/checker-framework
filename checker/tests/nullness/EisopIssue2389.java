// @below-java9-jdk-skip-test
// Test case for https://github.com/eisop/checker-framework/issues/2389
// A diamond on an anonymous subclass of an inner class, inside another inner class: the enclosing
// instance of the superclass is not the enclosing instance of the anonymous class.

import org.checkerframework.checker.nullness.qual.Nullable;

public class EisopIssue2389<K> {
    class It<T> {
        T t;

        It(T t) {
            this.t = t;
        }
    }

    class Keys {
        It<K> it(K k) {
            return new It<>(k) {};
        }

        It<@Nullable Object> nullable(@Nullable Object n) {
            return new It<>(n) {};
        }

        void deref(@Nullable Object n) {
            // :: error: (dereference.of.nullable)
            new It<>(n) {}.t.toString();
            new It<>("") {}.t.toString();
        }

        void notInferred(@Nullable Object n) {
            // :: error: (type.arguments.not.inferred)
            It<Object> o = new It<>(n) {};
        }

        class Deeper {
            It<String> it() {
                return new It<>("") {};
            }
        }
    }
}
