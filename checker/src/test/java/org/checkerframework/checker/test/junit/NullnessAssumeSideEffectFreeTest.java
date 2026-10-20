package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker with {@code -AassumeSideEffectFree} only. */
public class NullnessAssumeSideEffectFreeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessAssumeSideEffectFreeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessAssumeSideEffectFreeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AassumeSideEffectFree");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-assume-side-effect-free"};
    }
}
