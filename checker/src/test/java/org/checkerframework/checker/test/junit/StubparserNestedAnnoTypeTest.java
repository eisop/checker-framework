package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Regression test for {@code AnnotationFileParser.processTypeDecl}'s member-processing switch: a
 * nested annotation type declaration (e.g. {@code Outer.Nested}) must be processed like a nested
 * class, interface, or enum, not silently ignored. See {@code
 * checker/tests/stubparser-nestedannotype/Outer.astub} for the construct this pins, which mirrors
 * the real annotated JDK's {@code com.sun.tools.javac.api.ClientCodeWrapper.Trusted} and {@code
 * java.lang.invoke.LambdaForm.Compiled}: both are nested annotation types whose own
 * {@code @Retention}/{@code @Target} meta-annotations the text parser dropped entirely, since the
 * {@code default} case in the member-processing switch only handles records.
 *
 * <p>{@code -AstubWarnIfNotFound} makes an ignored member produce a diagnostic; this test expects
 * none, so a regression here would fail the test with an unexpected "AnnotationFileParser ignoring"
 * warning.
 */
public class StubparserNestedAnnoTypeTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserNestedAnnoTypeTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserNestedAnnoTypeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "stubparser-nestedannotype",
                "-Astubs=tests/stubparser-nestedannotype",
                "-AstubWarnIfNotFound");
    }

    /**
     * Returns the test directories.
     *
     * @return the test directories
     */
    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser-nestedannotype"};
    }
}
