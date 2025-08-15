/*
 * @test
 * @summary Test case for CollectionToArrayHeuristics.java.
 * @comment Replaces test cases from Issue 1929: https://github.com/typetools/checker-framework/issues/1929
 * @comment see related junit test: checker/tests/nullness/ToArrayFromField.java
 * @compile/fail/ref=ToArrayHeuristic-notrust.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker ToArrayHeuristic.java
 * @compile/fail/ref=ToArrayHeuristic-trust.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint=trustArrayLenZero ToArrayHeuristic.java
 */

import org.checkerframework.common.value.qual.ArrayLen;
import java.util.Collection;

public class ToArrayHeuristic {
    private final String[] EMPTY_STRING_ARRAY_LITERAL = new String[0];
    private final String[] EMPTY_STRING_ARRAY_BRACES = {};
    private final String[] NON_EMPTY_STRING_ARRAY = new String[1];
    String @ArrayLen(0) [] EMPTY_STRING_ARRAY_TRUSTED = {};
    String [] EMPTY_STRING_ARRAY_MISSING_MODIFIERS = {};

    String[] literalZero(Collection<String> c) {
        return c.toArray(new String[0]);
    }

    String[] literalBraces(Collection<String> c) {
        return c.toArray(new String[] {});
    }

    String[] literalReceiverSize(Collection<String> c) {
        return c.toArray(new String[c.size()]);
    }

    String[] fieldZero(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_LITERAL);
    }

    String[] fieldWithParens(Collection<String> c) {
        return c.toArray((EMPTY_STRING_ARRAY_LITERAL));
    }

    String[] fieldBraces(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_BRACES);
    }

    String[] fieldNonZero(Collection<String> c) {
        return c.toArray(NON_EMPTY_STRING_ARRAY);
    }

    String[] fieldUnsafe(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_TRUSTED);
    }

    String[] argWithParens(Collection<String> c) {
        return c.toArray(new String[(0)]);
    }

    String[] receiverSizeWithParens(Collection<String> c) {
	return (c).toArray(new String[((c)).size()]);
    }
}
