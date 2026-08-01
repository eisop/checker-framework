package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker when {@code -AwarnUnneededSuppressions} is used. */
public class NullnessWarnUnneededSuppressionsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessWarnUnneededSuppressionsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessWarnUnneededSuppressionsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness-warnunneededsuppressions",
                "-AwarnUnneededSuppressions");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-warnunneededsuppressions"};
    }
}
