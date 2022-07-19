package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class ConservativeDefaultsTest extends CheckerFrameworkPerDirectoryTest {

    public ConservativeDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "conservative-defaults",
                "-AuseConservativeDefaultsForUncheckedCode=bytecode");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"conservative-defaults"};
    }
}
