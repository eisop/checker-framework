package org.checkerframework.framework.test.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestDiagnosticUtilsTest {

    @Test
    public void testEmptyDiagnosticPosition() {
        String diagnosticString = "messageKey $$ 0 $$  $$ readableMessage";

        TestDiagnostic diagnostic = TestDiagnosticUtils.fromDiagnosticFileString(diagnosticString);

        assertNotNull(diagnostic);
        assertEquals("messageKey", diagnostic.getMessageKey());
    }
}
