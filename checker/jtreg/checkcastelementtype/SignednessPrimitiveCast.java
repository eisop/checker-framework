/*
 * @test
 * @summary Test that -AcheckCastElementType does not change a cast between two primitive types.
 * @compile/ref=SignednessPrimitiveCast.out -XDrawDiagnostics -processor org.checkerframework.checker.signedness.SignednessChecker -AcheckCastElementType SignednessPrimitiveCast.java
 */

import org.checkerframework.checker.signedness.qual.Unsigned;

public class SignednessPrimitiveCast {

    char narrow(int x) {
        return (char) x;
    }

    int widen(char c) {
        return (int) c;
    }

    long widenToLong(int x) {
        return (long) x;
    }

    int narrowFromLong(long x) {
        return (int) x;
    }

    double toDouble(int x) {
        return (double) x;
    }

    // The option is in effect: a cast to an array type, whose element type the expression's type
    // says nothing about, is still reported.
    @Unsigned int[] unverifiable(Object o) {
        return (@Unsigned int[]) o;
    }
}
