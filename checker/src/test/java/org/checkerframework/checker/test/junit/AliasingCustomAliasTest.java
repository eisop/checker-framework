package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Aliasing Checker's handling of an aliased {@code @Unique} annotation. */
public class AliasingCustomAliasTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an AliasingCustomAliasTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public AliasingCustomAliasTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "custom-alias-aliasing",
                "-AaliasedTypeAnnos=org.checkerframework.common.aliasing.qual.Unique:"
                        + "customaliasingalias.Unique");
    }

    /**
     * Returns the directories containing test code.
     *
     * @return the directories containing test code
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"custom-alias-aliasing"};
    }
}
