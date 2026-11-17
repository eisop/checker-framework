package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker -- testing {@code -AonlyDefs} command-line argument. */
public class NullnessOnlyDefsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessOnlyDefsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessOnlyDefsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AonlyDefs=^CheckMe");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-onlydefs"};
    }
}
