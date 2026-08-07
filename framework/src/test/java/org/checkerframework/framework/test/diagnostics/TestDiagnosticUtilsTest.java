package org.checkerframework.framework.test.diagnostics;

import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link TestDiagnosticUtils}. */
public class TestDiagnosticUtilsTest {

    /** Tests parsing a diagnostic string with an empty position. */
    @Test
    public void testEmptyDiagnosticPosition() {
        String diagnosticString = "messageKey $$ 0 $$  $$ readableMessage";

        TestDiagnostic diagnostic = TestDiagnosticUtils.fromDiagnosticFileString(diagnosticString);

        Assert.assertNotNull(diagnostic);
        Assert.assertEquals("messageKey", diagnostic.getMessageKey());
    }
}
