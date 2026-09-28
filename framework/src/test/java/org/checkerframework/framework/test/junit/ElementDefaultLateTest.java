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
 * Tests that {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault} reports a type
 * system error when it is called after type checking has begun, rather than silently applying the
 * new default to only the part of the program that has not been checked yet.
 */
public class ElementDefaultLateTest {

    /** Creates a new ElementDefaultLateTest. */
    public ElementDefaultLateTest() {}

    /** Runs the checker with the option that makes it add an element default too late. */
    @Test
    public void addElementDefaultAfterCheckingBeginsIsATypeSystemError() {
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/elementdefault",
                        TestUtilities.findNestedJavaTestFiles("elementdefault"),
                        Collections.singletonList(ElementDefaultChecker.class.getName()),
                        Arrays.asList(
                                "-A" + ElementDefaultAnnotatedTypeFactory.LATE_OPTION,
                                "-AnoPrintErrorStack"),
                        false);
        CompilationResult result = new TypecheckExecutor().compile(config);

        StringBuilder output = new StringBuilder(result.getJavacOutput());
        result.getDiagnostics().forEach(d -> output.append(d.getMessage(null)).append('\n'));
        String outputString = output.toString();

        Assert.assertFalse(
                "Compilation should have failed, but it succeeded. Output: " + outputString,
                result.compiledWithoutError());
        Assert.assertTrue(
                "Expected a message about addElementDefault being called too late, but got: "
                        + outputString,
                outputString.contains("was called after type checking began"));
    }
}
