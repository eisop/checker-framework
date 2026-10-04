package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests suppression of diagnostics reported on elements. */
public class ElementSuppressionTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public ElementSuppressionTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.framework.testchecker.elementsuppression
                        .ElementSuppressionChecker.class,
                "elementsuppression",
                "-AuseConservativeDefaultsForUncheckedCode=source,bytecode");
    }

    /**
     * @return the directories containing test files
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"conservative-defaults/elementsuppression"};
    }
}
