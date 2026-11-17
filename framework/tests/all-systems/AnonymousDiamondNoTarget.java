// @below-java9-jdk-skip-test
// An anonymous class created with a diamond whose result has no target type. The getDummyAssignedTo
// of the Value, Called Methods and Formatter Checkers returns the type of the expression, which is
// the anonymous class; inference must use its supertype as the target.
public class AnonymousDiamondNoTarget {
    static class Nested<T> {
        T t;

        // This constructor leaves the field unset.
        @SuppressWarnings({"initialization", "initializedfields:contracts.postcondition"})
        Nested() {}

        Nested(T t) {
            this.t = t;
        }
    }

    void noArg() {
        Nested<Object> a = new Nested<>() {};
        new Nested<>() {};
    }

    void arg(Object n) {
        Nested<Object> a = new Nested<>(n) {};
    }

    void argNoUse(Object n) {
        new Nested<>(n) {};
    }

    void argField(Object n) {
        Object a = new Nested<>(n) {}.t;
    }

    void argMethod(String s) {
        int l = new Nested<>(s) {}.t.length();
    }

    class Inner<U> {
        U u;

        Inner(U u) {
            this.u = u;
        }

        // This constructor leaves the field unset.
        @SuppressWarnings({"initialization", "initializedfields:contracts.postcondition"})
        Inner() {}
    }

    void innerNoTarget(AnonymousDiamondNoTarget other) {
        new Inner<>("a") {};
        new Inner<>() {};
        Object o = other.new Inner<>(1) {}.u;
        String s = new Inner<>("a") {}.toString();
    }

    void localClass() {
        class Local<V> extends Inner<V> {
            Local(V v) {
                super(v);
            }
        }
        new Local<>("x") {};
    }

    interface Box<T> {
        T get();
    }

    void iface() {
        new Box<>() {
            public String get() {
                return "";
            }
        }.get();
    }
}
