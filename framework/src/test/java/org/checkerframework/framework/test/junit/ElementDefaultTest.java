package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Tests that a default added via {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault} on a package
 * reaches that package's subpackages, the same way a written {@code @DefaultQualifier} would (see
 * eisop#2037).
 */
public class ElementDefaultTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * @param testFiles the files containing test code, which will be type-checked
     */
    public ElementDefaultTest(List<File> testFiles) {
        super(testFiles, ElementDefaultChecker.class, "elementdefault");
    }

    /**
     * Define the test directories for this test.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"elementdefault"};
    }
}
