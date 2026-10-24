// An offset in an upper bound annotation that is an expression with an error used to crash the
// Upper Bound Checker with a NullPointerException in UBQualifier.LessThanLengthOf.

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.SubstringIndexFor;

public class UBOffsetWithError {
    // :: error: (expression.unparsable.type.invalid)
    void unparsable(int[] a, @LTLengthOf(value = "#1", offset = "#$") int i) {
        int j = i;
    }

    // :: error: (expression.unparsable.type.invalid)
    void unknownIdentifier(int[] a, @LTLengthOf(value = "#1", offset = "b") int i) {
        int j = i;
    }

    // :: error: (expression.unparsable.type.invalid)
    @LTLengthOf(value = "this", offset = "#$") int field;

    // :: error: (expression.unparsable.type.invalid)
    @LTLengthOf(value = "#1", offset = "#$") int returnType(int[] a) {
        return 0;
    }

    // :: error: (expression.unparsable.type.invalid)
    void substringIndex(String s, @SubstringIndexFor(value = "#1", offset = "#$") int i) {
        int j = i;
    }

    // The loop over the sequences runs past the first element, which has a valid offset.
    void mixed(
            int[] a,
            int[] b,
            @LTLengthOf(
                            value = {"#1", "#2"},
                            offset = {"1", "#$"})
                    // :: error: (expression.unparsable.type.invalid)
                    int i) {
        int j = i;
    }
}
