package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class ResourceLeakCustomIgnoredExceptionsTest extends CheckerFrameworkPerDirectoryTest {
    public ResourceLeakCustomIgnoredExceptionsTest(List<File> testFiles) {
        super(
                testFiles,
                ResourceLeakChecker.class,
                "resourceleak-customignoredexceptions",
                "-AresourceLeakIgnoredExceptions=java.lang.Error, =java.lang.NullPointerException",
                "-AwarnUnneededSuppressions",
                "-encoding",
                "UTF-8");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"resourceleak-customignoredexceptions"};
    }
}
