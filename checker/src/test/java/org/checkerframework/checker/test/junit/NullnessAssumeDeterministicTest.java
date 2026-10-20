package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker with {@code -AassumeDeterministic} only. */
public class NullnessAssumeDeterministicTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessAssumeDeterministicTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessAssumeDeterministicTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AassumeDeterministic");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-assume-deterministic"};
    }
}
