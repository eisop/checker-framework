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
 * java.lang.invoke.LambdaForm.Compiled}: both are nested annotation types that carry annotations
 * supplied only by the stub. The {@code default} case in the switch handles only records, so a
 * nested annotation type is processed only because of the {@code ANNOTATION_TYPE} case.
 *
 * <p>The stub gives the parameter of {@code Outer.Nested.Helper.m} a {@code @Nullable} type, and
 * {@code -AmergeStubsWithSource} merges that onto the source declaration. Reaching that parameter
 * requires the parser to descend into the nested annotation type declaration and then its member
 * class, so {@code Outer.java}'s {@code h.m(null)} type-checks only when the nested declaration is
 * processed; a regression drops the qualifier and the call becomes an {@code
 * argument.type.incompatible} error.
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
                "-AmergeStubsWithSource",
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
