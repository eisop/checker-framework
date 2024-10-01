package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

public class ArrayElementAnnotationTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create an ArrayElementAnnotationTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public ArrayElementAnnotationTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "array-element-annotation");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"array-element-annotation"};
    }
}
