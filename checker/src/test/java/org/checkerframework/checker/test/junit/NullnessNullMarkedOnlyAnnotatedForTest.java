package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
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

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-nullmarked-onlyannotatedfor"};
    }
}
