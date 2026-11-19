package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests {@code SourceChecker#shouldSkipDefs(VariableTree)}. */
public class SkipDefsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public SkipDefsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.framework.testchecker.skipdefs.SkipDefsChecker.class,
                "skipdefs");
    }

    /**
     * @return the directories containing test files
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"skipdefs"};
    }
}
