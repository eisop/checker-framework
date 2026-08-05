package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/** JUnit tests for the Mutability Checker. */
public class MutabilityMutableDefaultTest extends CheckerFrameworkPerDirectoryTest {
    /**
     * Create a MutabilityMutableDefaultTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public MutabilityMutableDefaultTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.mutability.MutabilityChecker.class,
                "pico-mutable-default");
    }

    /**
     * Returns the test files for the Mutability Checker. The test files are in the
     * "tests/pico-mutable-default" directory.
     */
    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"pico-mutable-default"};
    }
}
