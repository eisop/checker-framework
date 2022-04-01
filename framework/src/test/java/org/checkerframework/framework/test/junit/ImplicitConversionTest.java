package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.implicitconversion.ImplicitConversionTestChecker;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

public class ImplicitConversionTest extends CheckerFrameworkPerDirectoryTest {

    public ImplicitConversionTest(List<File> testFiles) {
        super(testFiles, ImplicitConversionTestChecker.class, "implicitconversion");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"implicitconversion"};
    }
}
