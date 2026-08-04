package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typedeclbounds.TypeDeclBoundsChecker;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.List;

/** Reproduction: type-use annotation on a stub-supplied type-parameter upper bound. */
public class StubParserBoundsTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubParserBoundsTest(List<File> testFiles) {
        super(
                testFiles,
                TypeDeclBoundsChecker.class,
                "stubparserbounds",
                "-Astubs=tests/stubparserbounds/typeparambound.astub");
    }

    /**
     * @return the directories containing test code
     */
    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparserbounds"};
    }
}
