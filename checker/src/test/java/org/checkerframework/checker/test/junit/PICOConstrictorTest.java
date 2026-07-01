package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/** Constrictor benchmark tests in PICO. Files converted from https://zenodo.org/records/11003108 */
public class PICOConstrictorTest extends CheckerFrameworkPerDirectoryTest {
    /**
     * Create a PICOConstrictorTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public PICOConstrictorTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.pico.PICOChecker.class,
                "pico-constrictor",
                "-AassumeInitialized");
    }

    /**
     * Returns the test files for the PICO Checker. The test files are in the
     * "tests/pico-constrictor" directory.
     */
    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"pico-constrictor"};
    }
}
