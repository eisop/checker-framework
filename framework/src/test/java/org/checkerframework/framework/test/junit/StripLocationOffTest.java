package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.striplocation.StripLocationChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Tests the striplocation test checker with the opt-in off (default behavior): a qualifier in an
 * invalid location is reported and still takes effect, producing a {@code bound.type.incompatible}
 * cascade.
 */
public class StripLocationOffTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new StripLocationOffTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StripLocationOffTest(List<File> testFiles) {
        super(testFiles, StripLocationChecker.class, "striplocation-off");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"striplocation-off"};
    }
}
