package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Checker Framework, using the {@link EvenOddChecker}. */
// See FrameworkJavacErrorsTest for tests that can contain Java errors.
public class FrameworkTest extends CheckerFrameworkPerDirectoryTest {

    public FrameworkTest(List<File> testFiles) {
        super(testFiles, EvenOddChecker.class, "framework");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"framework", "all-systems"};
    }
}
