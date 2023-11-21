package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness Checker with the Initialization Checker.
 *
 * <p>Since the Initialization Checker cannot be run by itself, this covers
 *
 * <ul>
 *   <li>test cases for the Nullness Checker that depend on the Initialization Checker (in directory
 *       {@code nullness-initialization}),
 *   <li>test cases for the Nullness Checker that should behave the same regardless of whether
 *       initialization checking is on or off (in directory {@code nullness}; these are run both by
 *       this test and by the {@link NullnessAssumeInitializedTest},
 *   <li>test cases for the Initialization Checker that do not involve any nullness annotations (in
 *       directory {@code initialization})
 * </ul>
 */
public class NullnessTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckPurityAnnotations",
                "-AconservativeArgumentNullnessAfterInvocation=true",
                "-Xlint:deprecation",
                "-Alint=soundArrayCreationNullness,"
                        + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {
            "nullness", "nullness-initialization", "initialization", "all-systems"
        };
    }
}
