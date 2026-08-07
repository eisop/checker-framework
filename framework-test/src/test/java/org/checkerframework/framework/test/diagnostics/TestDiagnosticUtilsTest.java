package org.checkerframework.framework.test.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/** Tests for {@link TestDiagnosticUtils}. */
public class TestDiagnosticUtilsTest {

    /** Tests parsing a diagnostic string with an empty position. */
    @Test
    public void testEmptyDiagnosticPosition() {
        String diagnosticString = "messageKey $$ 0 $$  $$ readableMessage";

        TestDiagnostic diagnostic = TestDiagnosticUtils.fromDiagnosticFileString(diagnosticString);

        assertNotNull(diagnostic);
        assertEquals("messageKey", diagnostic.getMessageKey());
    }
}
