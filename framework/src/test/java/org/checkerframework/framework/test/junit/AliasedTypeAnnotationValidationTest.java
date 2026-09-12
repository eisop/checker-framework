package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorAnnotatedTypeFactory;
import org.checkerframework.framework.testchecker.aliasedctor.AliasedCtorChecker;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

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
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/aliasedctor",
                        TestUtilities.findNestedJavaTestFiles("aliasedctor"),
                        Collections.singletonList(AliasedCtorChecker.class.getName()),
                        Arrays.asList("-A" + option, "-AnoPrintErrorStack"),
                        false);
        CompilationResult result = new TypecheckExecutor().compile(config);

        StringBuilder output = new StringBuilder(result.getJavacOutput());
        result.getDiagnostics().forEach(d -> output.append(d.getMessage(null)).append('\n'));
        String outputString = output.toString();

        Assert.assertFalse(
                "Compilation should have failed, but it succeeded. Output: " + outputString,
                result.compiledWithoutError());
        return outputString;
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
}
