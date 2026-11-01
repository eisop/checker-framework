import org.checkerframework.common.value.qual.IntVal;

// A type variable without a qualifier was an invalid type for the Value Checker, so checks that
// compare it, such as the return check, were skipped without an error.
public class TypeVariableReturn {
    <T extends Integer> T wrong(@IntVal(2) T b) {
        // :: error: (return.type.incompatible)
        return b;
    }

    <T extends Integer> T fine(T a) {
        return a;
    }
}
