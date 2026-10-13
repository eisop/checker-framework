// Test case for https://github.com/eisop/checker-framework/issues/2106
// The type arguments of an inner class type such as Outer<T>.Inner include those of its enclosing
// type.  Type argument inference ignored them, so it crashed on consume(make("hello")).

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class EisopIssue2106 {
    class Outer<T> {
        class Inner {}

        class Mid<U> {
            class Deep {}
        }
    }

    static <T> Outer<T>.Inner make(T t) {
        throw new Error();
    }

    static <T, U> Outer<T>.Mid<U>.Deep makeDeep(T t, U u) {
        throw new Error();
    }

    static <T> List<Outer<T>.Inner> makeList(T t) {
        throw new Error();
    }

    static <S> void consume(Outer<S>.Inner x) {}

    static <S> S get(Outer<S>.Inner x) {
        throw new Error();
    }

    static <S> S getFromList(List<Outer<S>.Inner> x) {
        throw new Error();
    }

    static <S, V> S getOuter(Outer<S>.Mid<V>.Deep x) {
        throw new Error();
    }

    static <S, V> V getMid(Outer<S>.Mid<V>.Deep x) {
        throw new Error();
    }

    void test(@Nullable String n) {
        consume(make("hello"));

        get(make("hello")).length();
        // :: error: (dereference.of.nullable)
        get(make(n)).length();

        getFromList(makeList("hello")).length();
        // :: error: (dereference.of.nullable)
        getFromList(makeList(n)).length();

        getOuter(makeDeep("hello", n)).length();
        // :: error: (dereference.of.nullable)
        getMid(makeDeep("hello", n)).length();
        // :: error: (dereference.of.nullable)
        getOuter(makeDeep(n, "hello")).length();
        getMid(makeDeep(n, "hello")).length();
    }

    // A raw type such as Outer.Inner requires an unchecked conversion.
    @SuppressWarnings({"rawtypes", "unchecked"})
    void raw(Outer.Inner r) {
        get(r);
        consume(r);
    }
}

// The return type of a diamond instance creation of an inner class has the type arguments of the
// type of the enclosing instance, not the type parameters of the enclosing class.
class EisopIssue2106Diamond<Y> {
    Y y;

    EisopIssue2106Diamond(Y y) {
        this.y = y;
    }

    class Inner<T> {
        T t;

        Inner(T t) {
            this.t = t;
        }

        Y outer() {
            return y;
        }
    }

    static class Sub extends EisopIssue2106Diamond<@Nullable String> {
        Sub() {
            super(null);
        }

        void m() {
            new Inner<>("a").t.length();
            // :: error: (dereference.of.nullable)
            new Inner<>("a").outer().length();
        }
    }

    static void test(
            EisopIssue2106Diamond<@Nullable String> n, EisopIssue2106Diamond<String> nn, Sub s) {
        n.new Inner<>("a").t.length();
        // :: error: (dereference.of.nullable)
        n.new Inner<>("a").outer().length();
        nn.new Inner<>("a").outer().length();
        // :: error: (dereference.of.nullable)
        s.new Inner<>("a").outer().length();
    }

    static void anonymousReceiver() {
        new EisopIssue2106Diamond<String>("") {}.new Inner<>("a").outer().length();
        // :: error: (dereference.of.nullable)
        new EisopIssue2106Diamond<@Nullable String>(null) {}.new Inner<>("a").outer().length();
    }
}
