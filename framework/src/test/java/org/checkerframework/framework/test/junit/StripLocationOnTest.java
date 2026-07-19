package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.striplocation.StripLocationChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Tests the striplocation test checker with the opt-in on ({@code
 * -AstripInvalidLocationQualifiers}): a qualifier in an invalid bound location is reported and then
 * stripped and re-defaulted, so no {@code bound.type.incompatible} cascade is reported.
 */
public class StripLocationOnTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Creates a new StripLocationOnTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StripLocationOnTest(List<File> testFiles) {
        super(
                testFiles,
                StripLocationChecker.class,
                "striplocation-on",
                "-AstripInvalidLocationQualifiers");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"striplocation-on"};
    }
}
