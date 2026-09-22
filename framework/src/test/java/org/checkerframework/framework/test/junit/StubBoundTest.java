package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.nontopdefault.StubBoundChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for stub-provided type-parameter bound widening/narrowing. */
public class StubBoundTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubBoundTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubBoundTest(List<File> testFiles) {
        super(
                testFiles,
                StubBoundChecker.class,
                "stubbound",
                "-Astubs=tests/stubbound/StubBounds.astub",
                "-AmergeStubsWithSource");
    }

    /**
     * Define the test directories for this test.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubbound"};
    }
}
