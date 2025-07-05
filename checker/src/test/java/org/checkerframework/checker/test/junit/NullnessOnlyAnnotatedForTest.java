package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness checker. This test suite tests -AonlyAnnotatedFor suppresses errors
 * and warnings from the Nullness Checker for code outside the scope of @AnnotatedFor annotation.
 */
public class NullnessOnlyAnnotatedForTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessOnlyAnnotatedForTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessOnlyAnnotatedForTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AonlyAnnotatedFor");
    }

    /**
     * This method returns the directory containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-onlyannotatedfor"};
    }
}
