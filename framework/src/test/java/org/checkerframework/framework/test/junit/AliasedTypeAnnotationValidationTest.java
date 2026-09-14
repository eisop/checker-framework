package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorAnnotatedTypeFactory;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorBottom;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorChecker;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorTop;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Tests that {@link
 * org.checkerframework.framework.type.AnnotatedTypeFactory#addAliasedTypeAnnotation} validates that
 * the canonical annotation is a supported qualifier and the alias is not already in the type
 * hierarchy.
 */
public class AliasedTypeAnnotationValidationTest {

    /** Creates a new AliasedTypeAnnotationValidationTest. */
    public AliasedTypeAnnotationValidationTest() {}

    /**
     * Runs the compilation with the given option and returns the combined output.
     *
     * @param option the option to pass to the checker
     * @return the combined compiler output and diagnostics
     */
    private String compileWithOption(String option) {
        return compileExpectingFailure("-A" + option);
    }

    /**
     * Runs the compilation with the given full options, requires that it failed, and returns the
     * combined output.
     *
     * @param options the options to pass to the checker, each including its leading {@code -A}
     * @return the combined compiler output and diagnostics
     */
    private String compileExpectingFailure(String... options) {
        CompilationResult result = compile(options);
        String outputString = outputOf(result);
        Assert.assertFalse(
                "Compilation should have failed, but it succeeded. Output: " + outputString,
                result.compiledWithoutError());
        return outputString;
    }

    /**
     * Runs the compilation with the given full options and requires that it succeeded.
     *
     * @param options the options to pass to the checker, each including its leading {@code -A}
     */
    private void compileExpectingSuccess(String... options) {
        CompilationResult result = compile(options);
        Assert.assertTrue(
                "Compilation should have succeeded, but it failed. Output: " + outputOf(result),
                result.compiledWithoutError());
    }

    /**
     * Compiles {@code framework/tests/aliasedctor} with the AliasedCtor checker.
     *
     * @param options the options to pass to the checker, each including its leading {@code -A}
     * @return the result of the compilation
     */
    private CompilationResult compile(String... options) {
        List<String> allOptions = new ArrayList<>(Arrays.asList(options));
        allOptions.add("-AnoPrintErrorStack");
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/aliasedctor",
                        TestUtilities.findNestedJavaTestFiles("aliasedctor"),
                        Collections.singletonList(AliasedCtorChecker.class.getName()),
                        allOptions,
                        false);
        return new TypecheckExecutor().compile(config);
    }

    /**
     * Returns the combined compiler output and diagnostics of a compilation.
     *
     * @param result the result of a compilation
     * @return the combined compiler output and diagnostics
     */
    private String outputOf(CompilationResult result) {
        StringBuilder output = new StringBuilder(result.getJavacOutput());
        result.getDiagnostics().forEach(d -> output.append(d.getMessage(null)).append('\n'));
        return output.toString();
    }

    /**
     * Tests that registering an alias with an unsupported canonical AnnotationMirror throws
     * TypeSystemError.
     */
    @Test
    public void testUnsupportedCanonicalMirror() {
        String output =
                compileWithOption(
                        AliasedCtorAnnotatedTypeFactory.UNSUPPORTED_CANONICAL_MIRROR_OPTION);
        Assert.assertTrue(
                "Expected TypeSystemError about canonical annotation not in hierarchy, but got: "
                        + output,
                output.contains("canonical annotation")
                        && output.contains("is not in type hierarchy"));
    }

    /**
     * Tests that registering an alias with an unsupported canonical Class throws TypeSystemError.
     */
    @Test
    public void testUnsupportedCanonicalClass() {
        String output =
                compileWithOption(
                        AliasedCtorAnnotatedTypeFactory.UNSUPPORTED_CANONICAL_CLASS_OPTION);
        Assert.assertTrue(
                "Expected TypeSystemError about canonical annotation not in hierarchy, but got: "
                        + output,
                output.contains("canonical annotation")
                        && output.contains("is not in type hierarchy"));
    }

    /**
     * Tests that registering an alias whose name is already a supported qualifier throws
     * TypeSystemError.
     */
    @Test
    public void testAliasIsQualifierName() {
        String output =
                compileWithOption(AliasedCtorAnnotatedTypeFactory.ALIAS_IS_QUALIFIER_NAME_OPTION);
        Assert.assertTrue(
                "Expected TypeSystemError about alias being in type hierarchy, but got: " + output,
                output.contains("alias") && output.contains("should not be in type hierarchy"));
    }

    /**
     * Tests that registering an alias whose class is already a supported qualifier throws
     * TypeSystemError.
     */
    @Test
    public void testAliasIsQualifierClass() {
        String output =
                compileWithOption(AliasedCtorAnnotatedTypeFactory.ALIAS_IS_QUALIFIER_CLASS_OPTION);
        Assert.assertTrue(
                "Expected TypeSystemError about alias being in type hierarchy, but got: " + output,
                output.contains("alias") && output.contains("should not be in type hierarchy"));
    }

    /**
     * Tests that a supported qualifier declared without a {@code @Target} meta-annotation is
     * reported rather than dereferenced as null.
     */
    @Test
    public void testSupportedQualifierWithoutTarget() {
        String output =
                compileWithOption(AliasedCtorAnnotatedTypeFactory.NO_TARGET_QUALIFIER_OPTION);
        Assert.assertTrue(
                "Expected TypeSystemError about a missing @Target, but got: " + output,
                output.contains("AliasedCtorNoTarget")
                        && output.contains("has no @Target meta-annotation"));
    }

    /**
     * Tests that {@code -AaliasedTypeAnnos} naming a canonical annotation that is a declaration
     * annotation rather than a type annotation is a UserError, not a TypeSystemError.
     */
    @Test
    public void testCommandLineCanonicalIsDeclarationAnnotation() {
        String output =
                compileExpectingFailure("-AaliasedTypeAnnos=java.lang.Deprecated:aliasedctor.Foo");
        Assert.assertTrue(
                "Expected UserError about a non-type annotation, but got: " + output,
                output.contains("is not a type annotation")
                        && output.contains("java.lang.Deprecated"));
        Assert.assertFalse(
                "A command-line mistake must not be reported as a type-system bug: " + output,
                output.contains("A type system implementation is buggy"));
    }

    /**
     * Tests that {@code -AaliasedTypeAnnos} naming a canonical annotation that declares no
     * {@code @Target} is a UserError rather than a NullPointerException.
     */
    @Test
    public void testCommandLineCanonicalWithoutTarget() {
        String output =
                compileExpectingFailure(
                        "-AaliasedTypeAnnos=org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorNoTarget:aliasedctor.Foo");
        Assert.assertTrue(
                "Expected UserError about a missing @Target, but got: " + output,
                output.contains("is not a type annotation")
                        && output.contains("has no @Target meta-annotation"));
    }

    /**
     * Tests that {@code -AaliasedTypeAnnos} naming an alias that is itself a supported qualifier is
     * a UserError, not a TypeSystemError.
     */
    @Test
    public void testCommandLineAliasIsSupportedQualifier() {
        String output =
                compileExpectingFailure(
                        "-AaliasedTypeAnnos="
                                + AliasedCtorBottom.class.getCanonicalName()
                                + ":"
                                + AliasedCtorTop.class.getCanonicalName());
        Assert.assertTrue(
                "Expected UserError about the alias being a qualifier, but got: " + output,
                output.contains("cannot be an alias for")
                        && output.contains("is itself a qualifier"));
        Assert.assertFalse(
                "A command-line mistake must not be reported as a type-system bug: " + output,
                output.contains("A type system implementation is buggy"));
    }

    /**
     * Tests that {@code -AaliasedTypeAnnos} naming a canonical qualifier that this checker does not
     * support is skipped rather than reported.
     *
     * <p>The option is global and every type factory in the checker hierarchy processes it, so an
     * alias written for another type system reaching this one is the normal case. See {@code
     * CustomAliasTest}, where the Nullness Checker's KeyFor subchecker sees aliases written for
     * {@code @NonNull}.
     */
    @Test
    public void testCommandLineUnsupportedCanonicalIsSkipped() {
        compileExpectingSuccess(
                "-AaliasedTypeAnnos=org.checkerframework.checker.nullness.qual.Nullable:aliasedctor.Foo");
    }
}
