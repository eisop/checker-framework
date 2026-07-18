package org.checkerframework.checker.test.junit;

import org.checkerframework.checker.optional.OptionalChecker;
import org.checkerframework.framework.test.CompilationResult;
import org.checkerframework.framework.test.TestConfiguration;
import org.checkerframework.framework.test.TestConfigurationBuilder;
import org.checkerframework.framework.test.TypecheckExecutor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Regression test guarding against a class of bug that every other Checker Framework JUnit test
 * masks: a {@code checker.reportWarning}/{@code reportError} call site that passes the wrong number
 * of arguments for its message key's {@code messages.properties} format string.
 *
 * <p>{@code TestConfigurationBuilder} always adds {@code -Anomsgtext} (see its Javadoc), which
 * makes {@code SourceChecker#report} skip {@code String.format}ing the message entirely, so such a
 * mismatch compiles and passes every other test, yet throws a {@code BugInCF}-wrapped {@code
 * MissingFormatArgumentException} for a user running the checker normally. (This is exactly what
 * {@code OptionalImplVisitor#checkConditionalStatementIsPresentGetCall} used to do for the {@code
 * prefer.map.and.orelse} key: it always passed 2 arguments, but the message format has 3 {@code %s}
 * placeholders.)
 *
 * <p>{@code -Adetailedmsgtext} does not skip formatting -- it is meant to be machine-parsable, so
 * it still calls {@code String.format} -- so this test uses it to compile a test input that is
 * known to trigger every {@code prefer.*} warning, and asserts that the Checker Framework did not
 * crash. This is more direct than comparing formatted messages against the expected-diagnostic
 * comments in the test input (as {@code CheckerFrameworkPerDirectoryTest} does with {@code
 * -Anomsgtext}): the "// :: warning: (key)" comment syntax those test inputs use does not match the
 * verbose, machine-parsable format that {@code -Adetailedmsgtext} produces, so reusing that
 * comparison here would fail for unrelated formatting reasons rather than for a genuine arity bug.
 */
public class OptionalMessageFormattingTest {

    /**
     * Compiles {@code Marks3c.java} -- which triggers the {@code prefer.map.and.orelse} and {@code
     * prefer.ifpresent} warnings, including the declaration-initializer pattern that used to crash
     * -- with {@code -Adetailedmsgtext}, and asserts that no diagnostic reports a Checker Framework
     * crash.
     */
    @Test
    public void warningMessagesFormatWithoutCrashing() {
        TestConfiguration config =
                TestConfigurationBuilder.buildDefaultConfiguration(
                        "tests/optional",
                        new File("tests/optional", "Marks3c.java"),
                        OptionalChecker.class,
                        Collections.singletonList("-Adetailedmsgtext"),
                        false);
        CompilationResult result = new TypecheckExecutor().compile(config);
        for (Diagnostic<? extends JavaFileObject> diagnostic : result.getDiagnostics()) {
            String message = diagnostic.getMessage(null);
            if (message != null && message.contains("Checker Framework crashed")) {
                Assert.fail("Checker Framework crashed while formatting a message: " + message);
            }
        }
    }
}
