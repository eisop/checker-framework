package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Tests that a constructor's own explicit annotation, written using an alias rather than the
 * canonical annotation, is recognized when computing the type of a constructor reference ({@code
 * Foo::new}), the same way the canonical annotation would be. See {@code
 * org.checkerframework.framework.util.AnnotatedTypes#copyOnlyExplicitConstructorAnnotations}.
 */
public class AliasedCtorTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public AliasedCtorTest(List<File> testFiles) {
        super(testFiles, AliasedCtorChecker.class, "aliasedctor");
    }

    /**
     * Define the test directories for this test.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aliasedctor"};
    }
}
