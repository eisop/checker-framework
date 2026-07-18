package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Regression test for {@code AnnotationFileParser.findVariableElement(FieldAccessExpr)}: a
 * declaration annotation's field-access value must resolve even when its scope is itself a field
 * access (e.g. {@code DefinedByExample.Api.COMPILER}, whose scope is {@code DefinedByExample.Api}),
 * not just when the scope is a plain name (e.g. {@code Api.COMPILER}). See {@code
 * checker/tests/stubparser-nestedscope/UsesDefinedBy.astub} for the construct this pins, which
 * mirrors {@code com.sun.tools.javac.file.JavacFileManager.setPathFactory(..)}'s
 * {@code @DefinedBy(DefinedBy.Api.COMPILER)} in the real annotated JDK.
 *
 * <p>{@code -AstubWarnIfNotFound} makes an unresolved stub annotation produce a diagnostic; this
 * test expects none, so a regression here would fail the test with an unexpected "unknown
 * annotation" warning.
 */
public class StubparserNestedScopeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserNestedScopeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserNestedScopeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "stubparser-nestedscope",
                "-Astubs=tests/stubparser-nestedscope",
                "-AstubWarnIfNotFound");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser-nestedscope"};
    }
}
