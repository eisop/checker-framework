package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;

/** JUnit tests for the Checker Framework, using the {@link EvenOddChecker}. */
// See framework/tests/framework/README for why this is a CFPerFileTest.
// See FrameworkTest for tests that do not contain Java errors.
public class FrameworkJavacErrorsTest extends CheckerFrameworkPerFileTest {

    public FrameworkJavacErrorsTest(File testFile) {
        super(testFile, EvenOddChecker.class, "framework");
    }

    @Parameters
    public static String[] getTestDirs() {
        // Do not include all-systems!
        return new String[] {"framework-javac-errors"};
    }
}
