package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness Checker -- testing the {@code -Alint=monotonicNonNullOnStatic}
 * command-line argument, which warns when {@code @MonotonicNonNull} is written on a {@code static}
 * field.
 */
public class NullnessMonotonicNonNullOnStaticTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessMonotonicNonNullOnStaticTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessMonotonicNonNullOnStaticTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Alint=monotonicNonNullOnStatic");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-monotonicnonnullonstatic"};
    }
}
