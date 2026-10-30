package org.checkerframework.errorprone;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/** Tests for {@link CheckerFrameworkDriver}. */
public class CheckerFrameworkDriverTest {

    /**
     * A misspelled Error Prone flag and a {@code -A} option of a selected checker are reported in
     * one configuration error, so that one compilation shows both mistakes.
     */
    @Test
    public void unsupportedAndDashAOptionsAreReportedTogether() throws Throwable {
        EisopContextAdapterTest.duringAnalyze(
                context -> {
                    try {
                        CheckerFrameworkDriver.create(
                                context,
                                Collections.singletonList(
                                        "org.checkerframework.checker.nullness.NullnessChecker"),
                                Collections.singletonMap("stub", "x.astub"),
                                null);
                        Assert.fail("expected a configuration error");
                    } catch (IllegalArgumentException e) {
                        String message = e.getMessage();
                        Assert.assertTrue(
                                message,
                                message.contains("not recognized by any selected checker"));
                        Assert.assertTrue(message, message.contains("[stub]"));
                        Assert.assertTrue(
                                message,
                                message.contains(
                                        "-AsuppressWarnings=nullness as"
                                                + " -XepOpt:eisopcf:suppressWarnings=nullness"));
                    }
                },
                "-AsuppressWarnings=nullness");
    }
}
