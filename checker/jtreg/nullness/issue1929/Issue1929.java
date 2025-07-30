/*
 * @test
 * @summary Test case for Issue 1929: test -Alint=trustArrayLenZero
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker Issue1929.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Alint=trustArrayLenZero Issue1929.java
 */

import org.checkerframework.common.value.qual.ArrayLen;

import java.util.Collection;

public class Issue1929 {

    String[] works1(Collection<String> c) {
        return c.toArray(new String[0]);
    }

    private static final String @ArrayLen(0) [] EMPTY_STRING_ARRAY_2 = new String[0];

    String[] fails2(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_2);
    }

    private static final String[] EMPTY_STRING_ARRAY_3 = new String[0];

    String[] fails3(Collection<String> c) {
        return c.toArray(EMPTY_STRING_ARRAY_3);
    }
}
