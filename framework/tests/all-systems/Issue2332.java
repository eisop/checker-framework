// Test case for EISOP issue #2332:
// https://github.com/eisop/checker-framework/issues/2332
// Increment, decrement, and binary numeric operations on a variable whose type is a type variable
// bounded by a boxed primitive type. javac accepts these: unboxing conversion applies through the
// bound of the type variable. (A compound assignment such as `x += 1` is rejected by javac.)

import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
public class Issue2332 {
    static <N extends Integer> N postfixInt(N x) {
        x++;
        x--;
        return x;
    }

    static <N extends Integer> N prefixInt(N x) {
        ++x;
        --x;
        return x;
    }

    static <N extends Integer> int binaryInt(N x, N y) {
        return x + 1 + y;
    }

    static <N extends Long> N postfixLong(N x) {
        x++;
        x--;
        return x;
    }

    static <N extends Long> N prefixLong(N x) {
        ++x;
        --x;
        return x;
    }

    static <N extends Long> long binaryLong(N x, N y) {
        return x + 1 + y;
    }

    static <N extends Integer> boolean compare(N x, N y) {
        return x < 1 && x >= y;
    }

    static <N extends Integer & java.io.Serializable> N intersection(N x) {
        x++;
        return x + 1 > 0 ? x : x;
    }

    static int wildcard(List<? extends Integer> l) {
        return l.get(0) + 1;
    }
}
