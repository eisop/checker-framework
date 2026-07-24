package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.intersectionperelement.IntersectionPerElementChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests the per-element intersection-bound test checker. */
public class IntersectionPerElementTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new IntersectionPerElementTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public IntersectionPerElementTest(List<File> testFiles) {
        super(testFiles, IntersectionPerElementChecker.class, "intersectionperelement");
    }

    /**
     * Returns the directories containing the test inputs.
     *
     * @return the directories containing the test inputs
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"intersectionperelement"};
    }
}
