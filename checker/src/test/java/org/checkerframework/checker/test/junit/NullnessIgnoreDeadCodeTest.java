package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker when the -AignoreDeadCode command-line argument is used. */
public class NullnessIgnoreDeadCodeTest extends CheckerFrameworkPerDirectoryTest {
    /**
     * Create a NullnessIgnoreDeadCodeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessIgnoreDeadCodeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness-ignoredeadcode",
                "-AignoreDeadCode");
    }

    /**
     * Returns an array of test directory paths for parameterized testing.
     *
     * @return an array containing the test directory names
     */
    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-ignoredeadcode"};
    }
}
