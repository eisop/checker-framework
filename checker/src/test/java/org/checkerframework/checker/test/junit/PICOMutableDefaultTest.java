package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/** JUnit tests for the PICO Checker. */
public class PICOMutableDefaultTest extends CheckerFrameworkPerDirectoryTest {
    /**
     * Create a PICOMutableDefaultTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public PICOMutableDefaultTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.pico.PICOChecker.class,
                "pico-mutable-default");
    }

    /**
     * Returns the test files for the PICO Checker. The test files are in the
     * "tests/pico-mutable-default" directory.
     */
    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"pico-mutable-default"};
    }
}
