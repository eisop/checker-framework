// An offset in an upper bound annotation that is an expression with an error used to crash the
// Upper Bound Checker with a NullPointerException in UBQualifier.LessThanLengthOf.

import org.checkerframework.checker.index.qual.LTLengthOf;

public class UBOffsetWithError {
    // :: error: (expression.unparsable.type.invalid)
    void unparsable(int[] a, @LTLengthOf(value = "#1", offset = "#$") int i) {
        int j = i;
    }

    // :: error: (expression.unparsable.type.invalid)
    void unknownIdentifier(int[] a, @LTLengthOf(value = "#1", offset = "b") int i) {
        int j = i;
    }
}
