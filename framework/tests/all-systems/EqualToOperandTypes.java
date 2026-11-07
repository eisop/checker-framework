// The operands of `==` and a switch selector and its case labels need not have a common subtype,
// and their greatest lower bound can be an intersection type. Refining an operand keeps its own
// type.
@SuppressWarnings("interning:not.interned") // Compares references with ==.
public class EqualToOperandTypes {
    interface I {}

    interface J {}

    int switchOnByteWithCharLabels(byte tag) {
        switch (tag) {
            case 'Z':
                return 0;
            case 'C':
                return 1;
            default:
                return 2;
        }
    }

    void primitives(byte b, char c, int i, long l, double d) {
        if (b == c) {
            byte b2 = b;
            char c2 = c;
        }
        if (i == l) {
            long l2 = l;
        }
        if (d == i) {
            double d2 = d;
        }
    }

    void interfaces(I i, J j) {
        if (i == j) {
            I i2 = i;
            J j2 = j;
        }
        I i3 = i;
    }

    <T, U> void typeVariables(T t, U u) {
        if (t == u) {
            T t2 = t;
            U u2 = u;
        }
    }

    <T extends I> void boundedTypeVariable(T t, J j) {
        if (t == j) {
            T t2 = t;
        }
    }
}
