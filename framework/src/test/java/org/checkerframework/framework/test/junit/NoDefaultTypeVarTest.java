package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class NoDefaultTypeVarTest extends CheckerFrameworkPerDirectoryTest {

    public NoDefaultTypeVarTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.framework.testchecker.nodefaulttypevar.NoDefaultTypeVarChecker
                        .class,
                "nodefaulttypevar",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nodefaulttypevar"};
    }
}
