package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the custom aliasing using Nullness checker. */
public class CustomAliasTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a CustomAliasTest with Nullness checker
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public CustomAliasTest(List<File> testFiles) {
        super(
            testFiles,
            org.checkerframework.checker.nullness.NullnessChecker.class,
            "custom-alias",
            "-Aaliases=tests/custom-alias");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"custom-alias"};
    }
}
