package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker. */
public class NullnessOnlyAnnotatedForTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessNullMarkedTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessOnlyAnnotatedForTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AonlyAnnotatedFor=true");
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
