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

    // Other boxed bounds, and the places where the CFG builder unboxes a condition or an operand.
    static <T extends Boolean> int ifCondition(T b, int x) {
        if (b) {
            return x;
        }
        return 0;
    }

    static <T extends Boolean> void assertCondition(T b) {
        assert b;
    }

    static <T extends Boolean> int whileCondition(T b) {
        int i = 0;
        while (b) {
            i++;
            if (i > 3) {
                break;
            }
        }
        return i;
    }

    static <T extends Boolean> int conditionalExpression(T b) {
        return b ? 1 : 0;
    }

    static <T extends Boolean> boolean logical(T b, boolean c) {
        return !b && (b || c) ^ (b == c);
    }

    static <T extends Boolean> boolean assignToBoolean(T b) {
        boolean c = b;
        return c;
    }

    static <T extends Double> double doubleArithmetic(T d) {
        return d * 2;
    }

    static <T extends Long> boolean longComparison(T l, long m) {
        return l == m;
    }

    static <T extends Byte> int byteShift(T b) {
        return b << 1;
    }

    static <T extends Integer> int unary(T i) {
        return -i + ~i + +i;
    }

    static <T extends Integer> String concatenation(T i) {
        return "" + i;
    }

    static <T extends Integer> long widening(T i) {
        long l = i;
        return l;
    }

    static <T extends Integer> int conditionalOperand(boolean b, T i, int j) {
        return b ? i : j;
    }

    // Both operands have a type variable type. With two boxed operands, == compares references
    // and unboxes nothing.
    static <T extends Boolean> boolean twoBooleans(T b1, T b2) {
        return (b1 || b2) && (b1 ^ b2) | (b1 == b2);
    }

    static <T extends Boolean, U extends Boolean> boolean twoBounds(T b1, U b2) {
        if (b1 || b2) {
            return !(b1 == b2);
        }
        return false;
    }

    static <T extends Integer> int twoIntegers(T i, T j) {
        return i + j;
    }

    static <T extends Integer, U extends Long> long twoNumericBounds(T i, U j) {
        return i + j;
    }
}
