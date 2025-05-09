package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.javaexpression.JavaExpressionChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class JavaExpressionCheckerTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public JavaExpressionCheckerTest(List<File> testFiles) {
        super(testFiles, JavaExpressionChecker.class, "javaexpression");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"javaexpression", "all-systems"};
    }
}
