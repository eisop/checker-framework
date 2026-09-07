package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness Checker's treatment of JSpecify's {@code @NullMarked} as an alias
 * for {@code @AnnotatedFor("nullness")}. This is separate from {@link NullnessNullMarkedTest}
 * because {@code -AonlyAnnotatedFor} suppresses every error outside an {@code @AnnotatedFor} scope,
 * which would make the tests in that class vacuous.
 */
public class NullnessNullMarkedOnlyAnnotatedForTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessNullMarkedOnlyAnnotatedForTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessNullMarkedOnlyAnnotatedForTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AonlyAnnotatedFor");
    }

    /**
     * This method returns the directories containing test code. Each directory will be type-checked
     * with {@code -AonlyAnnotatedFor}.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-nullmarked-onlyannotatedfor"};
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
