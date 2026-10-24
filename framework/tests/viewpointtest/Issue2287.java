// Test case for eisop#2287: viewpoint adaptation crashed for an override of, or a raw method
// reference to, a method whose type variable is bounded by a class type variable.

import java.util.function.Function;

import viewpointtest.quals.*;

public class Issue2287 {
    abstract static class Ord<T> {
        <S extends T> Ord<S> rev() {
            return null;
        }

        <S extends T> S put(S s) {
            return s;
        }
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
    }

    // The bound T is adapted to @A, so a bound of @A is the same and a bound of @B is not.
    static class AOrd extends Ord<@A Object> {
        @Override
        <S extends @A Object> S put(S s) {
            return s;
        }
    }

    static class BOrd extends Ord<@A Object> {
        @Override
        // :: error: (override.typaram.invalid)
        <S extends @B Object> S put(S s) {
            return s;
        }
    }

    void rawMethodReference() {
        Function<Ord<@A Object>, Ord<@A Object>> rev = Ord::rev;
        // S extends @A Object cannot be @B Object.
        // :: error: (type.arguments.not.inferred)
        Function<Ord<@A Object>, Ord<@B Object>> bad = Ord::rev;
    }
}
