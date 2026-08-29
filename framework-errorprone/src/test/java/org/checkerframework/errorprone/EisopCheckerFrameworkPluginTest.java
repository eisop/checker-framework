package org.checkerframework.errorprone;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.scanner.ScannerSupplier;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

/**
 * End-to-end tests for {@link EisopCheckerFrameworkPlugin}: running Checker Framework type
 * system(s) as an Error Prone plugin over small source files and observing the diagnostics.
 *
 * <p>Findings are reported as Error Prone {@code Description}s for the {@code eisopcf} check, so
 * they honor Error Prone severity and {@code @SuppressWarnings("eisopcf")}. {@link
 * CompilationTestHelper} collects all compiler diagnostics, so the {@code // BUG: Diagnostic
 * contains:} markers match against the emitted {@code eisopcf} messages.
 */
public class EisopCheckerFrameworkPluginTest {

    /**
     * Fully-qualified name of the Nullness Checker (lives in the :checker module, test-only dep).
     */
    private static final String NULLNESS_CHECKER =
            "org.checkerframework.checker.nullness.NullnessChecker";

    /**
     * Fully-qualified name of the Interning Checker (lives in the :checker module, test-only dep).
     */
    private static final String INTERNING_CHECKER =
            "org.checkerframework.checker.interning.InterningChecker";

    /**
     * javac flags Error Prone requires. The --add-exports/--add-opens needed to run in-process are
     * supplied as JVM args by the module's build.gradle (they have no effect as compiler args).
     */
    private static final java.util.List<String> BASE_ARGS =
            Arrays.asList(
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true");

    private CompilationTestHelper helper;

    @Before
    public void setUp() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        helper =
                CompilationTestHelper.newInstance(scanner, getClass())
                        .setArgs(append(BASE_ARGS, "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER));
    }

    private static java.util.List<String> append(java.util.List<String> base, String... extra) {
        java.util.List<String> result = new java.util.ArrayList<>(base);
        result.addAll(Arrays.asList(extra));
        return result;
    }

    /**
     * The Nullness Checker, run via the {@code eisopcf} Error Prone plugin, reports returning
     * {@code null} from a non-{@code @Nullable} method.
     */
    @Test
    public void nullnessCheckerReportsBadReturn() {
        helper.addSourceLines(
                        "test/Bad.java",
                        "package test;",
                        "class Bad {",
                        "  // BUG: Diagnostic contains: return.type.incompatible",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }

    /** A correct program produces no Nullness Checker diagnostics. */
    @Test
    public void cleanProgramHasNoDiagnostics() {
        helper.expectNoDiagnostics()
                .addSourceLines(
                        "test/Good.java",
                        "package test;",
                        "class Good {",
                        "  String m() { return \"hello\"; }",
                        "}")
                .doTest();
    }

    /**
     * A finding is suppressed by {@code @SuppressWarnings("eisopcf")} on the enclosing class,
     * confirming the finding is reported as an Error Prone {@code Description} for the {@code
     * eisopcf} check (not merely printed through the Checker Framework's own Messager).
     */
    @Test
    public void findingIsSuppressibleViaErrorProne() {
        helper.expectNoDiagnostics()
                .addSourceLines(
                        "test/Suppressed.java",
                        "package test;",
                        "@SuppressWarnings(\"eisopcf\")",
                        "class Suppressed {",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }

    /**
     * With the {@code eisopcf} check overridden to ERROR severity, the finding is still reported at
     * the expected location, confirming Error Prone's per-check severity override is accepted for
     * Checker Framework findings. ({@link CompilationTestHelper} matches the {@code // BUG:} marker
     * regardless of the diagnostic's severity.)
     */
    @Test
    public void severityOverrideIsAccepted() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        CompilationTestHelper errorHelper =
                CompilationTestHelper.newInstance(scanner, getClass())
                        .setArgs(
                                append(
                                        BASE_ARGS,
                                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                                        "-Xep:eisopcf:ERROR"));
        errorHelper
                .addSourceLines(
                        "test/Err.java",
                        "package test;",
                        "class Err {",
                        "  // BUG: Diagnostic contains: return.type.incompatible",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }

    /**
     * Two type systems selected together (comma-separated) both run over one compilation, each
     * reporting its own findings. This is the "shared AST, per-checker CFG" model: one javac /
     * Error Prone invocation over one attributed AST, with each type system building its own CFG
     * (as standalone {@code javac -processor A,B} also does).
     */
    @Test
    public void multipleCheckersRunTogether() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        CompilationTestHelper multiHelper =
                CompilationTestHelper.newInstance(scanner, getClass())
                        .setArgs(
                                append(
                                        BASE_ARGS,
                                        "-XepOpt:eisopcf:checkers="
                                                + NULLNESS_CHECKER
                                                + ","
                                                + INTERNING_CHECKER));
        multiHelper
                .addSourceLines(
                        "test/Two.java",
                        "package test;",
                        "class Two {",
                        "  // BUG: Diagnostic contains: return.type.incompatible",
                        "  String m() { return null; }",
                        "  boolean eq(Object a, Object b) {",
                        "    // BUG: Diagnostic contains: not.interned",
                        "    return a == b;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * An unresolvable checker name produces a clear error rather than silently doing nothing,
     * confirming the reflective checker resolution surfaces mistakes.
     */
    @Test
    public void unknownCheckerNameIsReported() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        CompilationTestHelper badHelper =
                CompilationTestHelper.newInstance(scanner, getClass())
                        .setArgs(
                                append(
                                        BASE_ARGS,
                                        "-XepOpt:eisopcf:checkers=com.example.NoSuchChecker"));
        badHelper
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
                        "// BUG: Diagnostic contains: Checker class not found",
                        "class Any {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }
}
