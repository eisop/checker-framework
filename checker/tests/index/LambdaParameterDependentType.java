// Dependent type annotations that name a lambda parameter used to crash with
// "Could not find parameter Element in parameter list".

import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.SameLen;

import java.util.function.BiFunction;
import java.util.function.Function;

public class LambdaParameterDependentType {
    void implicitParam() {
        BiFunction<int[], Integer, Integer> f =
                (a, x) -> {
                    @LTEqLengthOf("a") int j = a.length;
                    return j;
                };
    }

    void explicitParam() {
        Function<int[], Integer> f =
                (int[] a) -> {
                    @LTEqLengthOf("a") int j = a.length;
                    return j;
                };
    }

    void sameLenLocal() {
        Function<int[], Integer> f =
                a -> {
                    int[] b = a;
                    int @SameLen("a") [] c = b;
                    return c.length;
                };
    }

    void nestedLambda() {
        Function<int[], Function<int[], Integer>> f =
                a ->
                        c -> {
                            @LTEqLengthOf("a") int j = a.length;
                            // :: error: (assignment.type.incompatible)
                            @LTEqLengthOf("a") int k = c.length;
                            return j + k;
                        };
    }

    void wrongParam() {
        BiFunction<int[], int[], Integer> f =
                (a, c) -> {
                    // :: error: (assignment.type.incompatible)
                    @LTEqLengthOf("a") int j = c.length;
                    return j;
                };
    }

    void methodParam(int[] m) {
        Function<int[], Integer> f =
                a -> {
                    @LTEqLengthOf("m") int j = m.length;
                    // :: error: (assignment.type.incompatible)
                    @LTEqLengthOf("m") int k = a.length;
                    return j + k;
                };
    }
}
