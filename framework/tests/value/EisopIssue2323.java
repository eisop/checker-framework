// Test case for https://github.com/eisop/checker-framework/issues/2323 :
// startsWith/endsWith give a fact about a value that has already been computed, so it must not
// be recorded for a receiver that is an impure call.

import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;

public class EisopIssue2323 {
    @Deterministic
    String impure() {
        return "";
    }

    @Pure
    String pure() {
        return "";
    }

    void testImpure() {
        if (impure().startsWith("abc")) {
            // :: error: (assignment.type.incompatible)
            @ArrayLenRange(from = 3) String s = impure();
        }
    }

    void testPure() {
        if (pure().startsWith("abc")) {
            @ArrayLenRange(from = 3) String s = pure();
        }
    }
}
