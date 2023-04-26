package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the custom aliasing. */
public class CustomAliasTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a CustomAliasTest with the Nullness Checker and the Purity Checker.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public CustomAliasTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "custom-alias",
                "-AaliasedTypeAnnos=org.checkerframework.checker.nullness.qual.NonNull:custom.alias.NonNull;"
                        + "org.checkerframework.checker.nullness.qual.Nullable:custom.alias.Nullable",
                "-AaliasedDeclAnnos=org.checkerframework.dataflow.qual.Pure:custom.alias.Pure",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"custom-alias"};
    }
}
