package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * JUnit tests for the Nullness Checker with {@code -AassumeSideEffectFree} and {@code
 * -AassumeDeterministic}.
 */
public class NullnessAssumePureTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessAssumePureTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessAssumePureTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AassumeSideEffectFree",
                "-AassumeDeterministic");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-assume-pure"};
    }
}
