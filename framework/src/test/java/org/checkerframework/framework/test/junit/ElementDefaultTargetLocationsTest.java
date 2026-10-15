package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TestUtilities;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultAnnotatedTypeFactory;
import org.checkerframework.framework.testchecker.elementdefault.ElementDefaultChecker;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests that programmatic defaults registered for a location prohibited by the qualifier's {@link
 * org.checkerframework.framework.qual.TargetLocations} meta-annotation are reported as type-system
 * errors.
 */
public class ElementDefaultTargetLocationsTest {

    /** Creates a new ElementDefaultTargetLocationsTest. */
    public ElementDefaultTargetLocationsTest() {}

    /**
     * Compiles the elementdefault tests with {@code option} passed to the checker and asserts that
     * compilation fails with an error about prohibited location.
     *
     * @param option the option to pass to the checker
     */
    private void runWithOption(String option) {
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/elementdefault",
                        TestUtilities.findNestedJavaTestFiles("elementdefault"),
                        Collections.singletonList(ElementDefaultChecker.class.getName()),
                        Arrays.asList("-A" + option, "-AnoPrintErrorStack"),
                        false);
        CompilationResult result = new TypecheckExecutor().compile(config);

        StringBuilder output = new StringBuilder(result.getJavacOutput());
        result.getDiagnostics().forEach(d -> output.append(d.getMessage(null)).append('\n'));
        String outputString = output.toString();

        Assert.assertFalse(
                "Compilation should have failed, but it succeeded. Output: " + outputString,
                result.compiledWithoutError());
        Assert.assertTrue(
                "Expected a message about prohibited location, but got: " + outputString,
                outputString.contains(
                        "is not permitted at location RETURN by its @TargetLocations"));
    }

    /**
     * Tests that registering a checked code default for a location prohibited by @TargetLocations
     * is reported as a type-system error.
     */
    @Test
    public void disallowedCheckedDefaultIsATypeSystemError() {
        runWithOption(ElementDefaultAnnotatedTypeFactory.DISALLOWED_CHECKED_OPTION);
    }

    /**
     * Tests that registering an unchecked code default for a location prohibited
     * by @TargetLocations is reported as a type-system error.
     */
    @Test
    public void disallowedUncheckedDefaultIsATypeSystemError() {
        runWithOption(ElementDefaultAnnotatedTypeFactory.DISALLOWED_UNCHECKED_OPTION);
    }

    /**
     * Tests that registering an element default for a location prohibited by @TargetLocations is
     * reported as a type-system error.
     */
    @Test
    public void disallowedElementDefaultIsATypeSystemError() {
        runWithOption(ElementDefaultAnnotatedTypeFactory.DISALLOWED_ELEMENT_OPTION);
    }

    /**
     * Tests that registering programmatic defaults on locations allowed
     * by @ProgrammaticDefaultLocations succeeds without prohibited location errors.
     */
    @Test
    public void programmaticAllowedDefaultSucceeds() {
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/elementdefault",
                        TestUtilities.findNestedJavaTestFiles("elementdefault"),
                        Collections.singletonList(ElementDefaultChecker.class.getName()),
                        Arrays.asList(
                                "-A"
                                        + ElementDefaultAnnotatedTypeFactory
                                                .PROGRAMMATIC_ALLOWED_OPTION,
                                "-AnoPrintErrorStack"),
                        false);
        CompilationResult result = new TypecheckExecutor().compile(config);

        StringBuilder output = new StringBuilder(result.getJavacOutput());
        result.getDiagnostics().forEach(d -> output.append(d.getMessage(null)).append('\n'));
        String outputString = output.toString();

        Assert.assertFalse(
                "Programmatic default with @ProgrammaticDefaultLocations should not fail with prohibited location, but got: "
                        + outputString,
                outputString.contains("is not permitted at location"));
    }
}
