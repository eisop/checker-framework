package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Lock Checker's handling of an aliased GuardedBy-hierarchy qualifier. */
public class LockCustomAliasTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a LockCustomAliasTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public LockCustomAliasTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.lock.LockChecker.class,
                "custom-alias-lock",
                "-AaliasedTypeAnnos=org.checkerframework.checker.lock.qual.GuardedByUnknown:"
                        + "customlockalias.GuardedByUnknown",
                // Ignore the test suite's usage of qualifiers in illegal locations.
                "-AignoreTargetLocations");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"custom-alias-lock"};
    }
}
