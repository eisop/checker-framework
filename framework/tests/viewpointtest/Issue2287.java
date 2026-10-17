// Test case for eisop#2287: viewpoint-adapting an overridden method, or a method referenced
// through a raw type, whose type variable is bounded by a class type variable crashed.

import java.util.function.Function;

public class Issue2287 {
    abstract static class Ord<T> {
        <S extends T> Ord<S> rev() {
            return null;
        }

        <S extends T> S put(S s) {
            return s;
        }

        T get() {
            return null;
        }

        void set(T t) {}
    }

    static class Rev<T> extends Ord<T> {
        @Override
        <S extends T> Ord<S> rev() {
            return null;
        }

        @Override
        <S extends T> S put(S s) {
            return s;
        }

        @Override
        T get() {
            return null;
        }

        @Override
        void set(T t) {}
    }

    Function<Ord<String>, Ord<String>> revField = Ord::rev;

    void rawMethodReference() {
        Function<Ord<String>, Ord<String>> rev = Ord::rev;
        Function<Ord<String>, String> get = Ord::get;
    }
}
