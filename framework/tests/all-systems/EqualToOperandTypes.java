// The operands of `==`, and a switch selector and its case labels, need not have a common subtype,
// and their greatest lower bound can be an intersection type. Refining an operand keeps its own
// Java type. No diagnostic depends on that type, so this test only checks that each checker
// handles these comparisons; computing the Java types' greatest lower bound crashed on the switch.
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

    void interfaces(I i, J j) {
        if (i == j) {}
    }

    <T, U> void typeVariables(T t, U u) {
        if (t == u) {}
    }

    <T extends I> void boundedTypeVariable(T t, J j) {
        if (t == j) {}
    }
}
