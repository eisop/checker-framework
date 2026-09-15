package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker under {@code -Amode=jspecify}. */
public class NullnessJSpecifyModeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessJSpecifyModeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessJSpecifyModeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Amode=jspecify");
    }

    /**
     * This method returns the directories containing test code. Each directory will be type-checked
     * with {@code -Amode=jspecify}.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-jspecify-mode"};
    }

    @Override
    @Test
    @SuppressWarnings("JUnitMethodInvoked")
    public void run() {
        /*
         * Skip under JDK 8: JSpecify's @NullMarked is meta-annotated
         * @Target({MODULE, PACKAGE, TYPE, METHOD, CONSTRUCTOR}), and ElementType.MODULE does not
         * exist before Java 9.  javac 8 therefore emits "unknown enum constant
         * java.lang.annotation.ElementType.MODULE" when it reads @NullMarked, which the test
         * harness counts as an unexpected diagnostic and fails on.
         */
        if (TestUtilities.IS_AT_LEAST_9_JVM) {
            super.run();
        }
    }
}
