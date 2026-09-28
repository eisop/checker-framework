package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.nodefaulttypevar.NoDefaultTypeVarChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Test case for reproducing and verifying the fix for the NullPointerException in
 * AnnotatedTypes.glbSubtype when encountering unannotated type variable bounds.
 */
public class NoDefaultTypeVarTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new NoDefaultTypeVarTest.
     *
     * @param testFiles the test files
     */
    public NoDefaultTypeVarTest(List<File> testFiles) {
        super(testFiles, NoDefaultTypeVarChecker.class, "nodefaulttypevar", "-nowarn");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nodefaulttypevar"};
    }
}
