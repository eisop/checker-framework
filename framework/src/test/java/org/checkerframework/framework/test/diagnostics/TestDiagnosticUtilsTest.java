package org.checkerframework.framework.test.diagnostics;

import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link TestDiagnosticUtils}. */
public class TestDiagnosticUtilsTest {

    /** Tests parsing a diagnostic string with an empty position. */
    @Test
    public void testEmptyDiagnosticPosition() {
        String diagnosticString = "Dummy.java:10: error: messageKey $$ 0 $$  $$ readableMessage";

        TestDiagnostic diagnostic = TestDiagnosticUtils.fromDiagnosticFileString(diagnosticString);

        Assert.assertNotNull(diagnostic);
        Assert.assertEquals("messageKey", diagnostic.getMessageKey());
        Assert.assertEquals(10, diagnostic.getLineNumber());
    }

    /** Tests parsing a diagnostic string with a malformed position (missing comma). */
    @Test
    public void testMalformedDiagnosticPosition() {
        String diagnosticString =
                "Dummy.java:10: error: messageKey $$ 0 $$ (10) $$ readableMessage";

        TestDiagnostic diagnostic = TestDiagnosticUtils.fromDiagnosticFileString(diagnosticString);

        Assert.assertNotNull(diagnostic);
        Assert.assertEquals("messageKey", diagnostic.getMessageKey());
    }

    /** Tests that an error key in parentheses and in square brackets are the same diagnostic. */
    @Test
    public void testKeyInParenthesesOrBrackets() {
        TestDiagnostic parens =
                TestDiagnosticUtils.fromJavaFileComment("Dummy.java", 5L, "error: (assignment)");
        TestDiagnostic brackets =
                TestDiagnosticUtils.fromJavaFileComment("Dummy.java", 5L, "error: [assignment]");
        TestDiagnostic bare =
                TestDiagnosticUtils.fromJavaFileComment("Dummy.java", 5L, "error: assignment");

        Assert.assertEquals("assignment", parens.getMessageKey());
        Assert.assertEquals("assignment", brackets.getMessageKey());
        Assert.assertEquals(parens, brackets);
        Assert.assertEquals(parens.hashCode(), brackets.hashCode());
        Assert.assertEquals(bare, brackets);
        // The delimiters that were written are kept for the output.
        Assert.assertEquals("Dummy.java:5: error: (assignment)", parens.toString());
        Assert.assertEquals("Dummy.java:5: error: [assignment]", brackets.toString());
        Assert.assertEquals("Dummy.java:5: error: assignment", bare.toString());
    }

    /** Tests that square brackets that are not the whole message are not taken as a key. */
    @Test
    public void testBracketsAroundLintCategory() {
        TestDiagnostic lint =
                TestDiagnosticUtils.fromJavaFileComment(
                        "Dummy.java", 5L, "warning: [unchecked] unchecked cast");

        Assert.assertEquals("[unchecked] unchecked cast", lint.getMessageKey());
    }
}
