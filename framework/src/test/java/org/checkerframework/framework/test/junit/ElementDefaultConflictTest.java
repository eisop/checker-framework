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
 * Tests that a default registered by {@link
 * org.checkerframework.framework.util.defaults.QualifierDefaults#addElementDefault} which conflicts
 * with a {@code @DefaultQualifier} written on the same declaration is reported as a type-system
 * error, rather than the two being merged and the winner decided by annotation ordering.
 */
public class ElementDefaultConflictTest {

    /** Creates a new ElementDefaultConflictTest. */
    public ElementDefaultConflictTest() {}

    /**
     * Runs the checker with the option that makes it register an element default conflicting with
     * the {@code @DefaultQualifier} written on {@code OrderBeforeClass}.
     */
    @Test
    public void conflictWithWrittenDefaultQualifierIsATypeSystemError() {
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/elementdefault",
                        TestUtilities.findNestedJavaTestFiles("elementdefault"),
                        Collections.singletonList(ElementDefaultChecker.class.getName()),
                        Arrays.asList(
                                "-A" + ElementDefaultAnnotatedTypeFactory.CONFLICT_OPTION,
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
                "Expected a message about conflicting defaults, but got: " + outputString,
                outputString.contains("Conflicting defaults on CLASS"));
        Assert.assertTrue(
                "Expected the conflicting declaration to be named, but got: " + outputString,
                outputString.contains("elementdefault.pkg.OrderBeforeClass"));
    }
}
