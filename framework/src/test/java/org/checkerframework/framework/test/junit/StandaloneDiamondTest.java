package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.standalonediamond.StandaloneDiamondChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests the dataflow value of a diamond nested in an enclosing type argument inference. */
public class StandaloneDiamondTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a StandaloneDiamondTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StandaloneDiamondTest(List<File> testFiles) {
        super(testFiles, StandaloneDiamondChecker.class, "standalone-diamond");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"standalone-diamond"};
    }
}
