package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness checker when checkCastElementType is used. */
public class NullnessCheckRedundantAnnotations extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessCheckCastElementTypeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessCheckRedundantAnnotations(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckRedundantAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-checkredundantannotations"};
    }
}
