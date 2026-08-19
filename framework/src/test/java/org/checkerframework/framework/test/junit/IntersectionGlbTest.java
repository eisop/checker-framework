package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.intersectionglb.IntersectionGlbChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests a checker that overrides how an intersection type's bound qualifiers are combined. */
public class IntersectionGlbTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates an IntersectionGlbTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public IntersectionGlbTest(List<File> testFiles) {
        super(testFiles, IntersectionGlbChecker.class, "intersectionglb");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"intersectionglb"};
    }
}
