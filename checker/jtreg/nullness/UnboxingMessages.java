/*
 * @test
 * @summary Test that an error about the unboxing of a possibly-null value names the possibly-null
 * expression: the operand of a cast to a primitive, each possibly-null operand of a conditional
 * expression, and an expression without its parentheses.
 *
 * @compile/fail/ref=UnboxingMessages.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker UnboxingMessages.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

class UnboxingMessages {
    void cast(@Nullable Object o) {
        int x = (int) o;
    }

    void conditional(boolean c, @Nullable Integer i, @Nullable Integer k) {
        int x = (c ? i : k) + 1;
    }

    void parenthesized(@Nullable Integer i) {
        int x = ((i)) + 1;
    }

    void condition(@Nullable Boolean b) {
        if ((b)) {}
    }

    void iterate(@Nullable List<Integer> list) {
        for (Integer x : (list)) {}
    }

    void element(List<@Nullable Integer> list) {
        for (int x : list) {}
    }
}
