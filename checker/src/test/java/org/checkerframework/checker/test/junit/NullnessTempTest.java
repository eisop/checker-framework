package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker. */
public class NullnessTempTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessTempTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessTempTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Alint=soundArrayCreationNullness,"
                        + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-temp"};
    }
}
