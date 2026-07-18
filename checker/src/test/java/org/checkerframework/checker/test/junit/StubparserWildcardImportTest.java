package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Regression test for {@code AnnotationFileParser.findVariableElement(FieldAccessExpr)}: a
 * declaration annotation whose value is a field access (e.g. {@code RetentionPolicy.RUNTIME}) must
 * resolve when the receiver type ({@code RetentionPolicy}) is reachable only through a wildcard
 * type import ({@code import java.lang.annotation.*;}), not just through an explicit single-type
 * import or a static import of the constant itself. See {@code
 * checker/tests/stubparser-wildcardimport/WildcardImportScope.astub} for the construct this pins,
 * which mirrors how the real annotated JDK's {@code java.lang.Override}, {@code
 * java.lang.Deprecated}, and {@code java.lang.SuppressWarnings} write their own
 * {@code @Retention}/{@code @Target} meta-annotations.
 *
 * <p>{@code -AstubWarnIfNotFound} makes an unresolved stub annotation produce a diagnostic; this
 * test expects none, so a regression here would fail the test with an unexpected "unknown
 * annotation" warning.
 */
public class StubparserWildcardImportTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserWildcardImportTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserWildcardImportTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "stubparser-wildcardimport",
                "-Astubs=tests/stubparser-wildcardimport",
                "-AstubWarnIfNotFound");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser-wildcardimport"};
    }
}
