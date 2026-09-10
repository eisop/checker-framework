package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness Checker's treatment of JSpecify's {@code @NullMarked} and
 * {@code @NullUnmarked} as {@code @AnnotatedFor} and {@code @UnannotatedFor} scopes, which it does
 * under {@code -Amode=jspecify}.
 */
public class NullnessJSpecifyScopeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessJSpecifyScopeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessJSpecifyScopeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Amode=jspecify");
    }

    /**
     * This method returns the directory containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-jspecifyscope"};
    }

    @Override
    @Test
    @SuppressWarnings("JUnitMethodInvoked")
    public void run() {
        // Skip under JDK8: the JSpecify artifact this test compiles against requires JDK9+.
        if (TestUtilities.IS_AT_LEAST_9_JVM) {
            super.run();
        }
    }
}
