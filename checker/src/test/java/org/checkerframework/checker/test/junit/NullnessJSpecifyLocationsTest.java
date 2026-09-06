package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness Checker when -AjspecifyUnrecognizedLocations is used. */
public class NullnessJSpecifyLocationsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessJSpecifyLocationsTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessJSpecifyLocationsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AjspecifyUnrecognizedLocations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-jspecify-locations"};
    }
}
