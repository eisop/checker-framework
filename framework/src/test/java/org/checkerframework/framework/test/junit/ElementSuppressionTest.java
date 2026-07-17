package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class ElementSuppressionTest extends CheckerFrameworkPerDirectoryTest {
    public ElementSuppressionTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.framework.testchecker.elementsuppression
                        .ElementSuppressionChecker.class,
                "elementsuppression",
                "-AuseConservativeDefaultsForUncheckedCode=source,bytecode");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"conservative-defaults/elementsuppression"};
    }
}
