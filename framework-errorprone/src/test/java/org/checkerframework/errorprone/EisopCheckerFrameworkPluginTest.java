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

    /** A {@link CompilationTestHelper} with both the Nullness and Interning checkers selected. */
    private CompilationTestHelper twoCheckerHelper() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        return CompilationTestHelper.newInstance(scanner, getClass())
                .setArgs(
                        append(
                                BASE_ARGS,
                                "-XepOpt:eisopcf:checkers="
                                        + NULLNESS_CHECKER
                                        + ","
                                        + INTERNING_CHECKER));
    }

    /**
     * Illustrates the suppression mechanisms available when several Checker Framework type systems
     * run under the single {@code eisopcf} Error Prone check. Both the Nullness and Interning
     * checkers are enabled.
     *
     * <p><b>Per-type-system suppression works at any granularity (method, class, ...)</b> using the
     * Checker Framework's own keys, because the Checker Framework applies its suppression
     * <em>before</em> a finding becomes an {@code eisopcf} Error Prone {@code Description}:
     *
     * <ul>
     *   <li>{@code @SuppressWarnings("nullness")} suppresses only Nullness Checker findings;
     *   <li>{@code @SuppressWarnings("interning")} suppresses only Interning Checker findings;
     *   <li>{@code @SuppressWarnings("allcheckers")} suppresses all Checker Framework findings.
     * </ul>
     *
     * <p><b>The Error Prone {@code "eisopcf"} key suppresses all Checker Framework findings</b> and
     * works at any enclosing declaration (class, method, local variable). Because the plugin visits
     * each top-level class and reports the enclosed type systems' findings from there, Error
     * Prone's per-node suppression does not automatically cover findings below the class; the
     * plugin reconstructs that suppression along each finding's path. That is asserted separately
     * by {@link #eisopcfKeySuppressesAtAnyDeclaration()}.
     */
    @Test
    public void perTypeSystemSuppression() {
        twoCheckerHelper()
                .addSourceLines(
                        "test/Suppression.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Suppression {",
                        // No suppression: both findings reported.
                        "  int none(@Nullable String s, Object a, Object b) {",
                        "    // BUG: Diagnostic contains: dereference.of.nullable",
                        "    int len = s.length();",
                        "    // BUG: Diagnostic contains: not.interned",
                        "    boolean eq = a == b;",
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        // Suppress only nullness (method level): interning finding still reported.
                        "  @SuppressWarnings(\"nullness\")",
                        "  int onlyNullness(@Nullable String s, Object a, Object b) {",
                        "    int len = s.length();", // nullness deref suppressed
                        "    // BUG: Diagnostic contains: not.interned",
                        "    boolean eq = a == b;",
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        // Suppress only interning (method level): nullness finding still reported.
                        "  @SuppressWarnings(\"interning\")",
                        "  int onlyInterning(@Nullable String s, Object a, Object b) {",
                        "    // BUG: Diagnostic contains: dereference.of.nullable",
                        "    int len = s.length();",
                        "    boolean eq = a == b;", // interning == suppressed
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        // Suppress all Checker Framework findings (method level) via the CF
                        // all-checkers key: both suppressed.
                        "  @SuppressWarnings(\"allcheckers\")",
                        "  int allViaAllcheckers(@Nullable String s, Object a, Object b) {",
                        "    int len = s.length();",
                        "    boolean eq = a == b;",
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * The Error Prone {@code "eisopcf"} suppression key takes effect at any enclosing declaration —
     * class, method, field, or local variable — like an ordinary Error Prone check. The plugin
     * reconstructs Error Prone's descent-based suppression along each finding's path (it cannot
     * rely on Error Prone's own per-node suppression, because it matches at the class and reports
     * findings for the whole subtree).
     */
    @Test
    public void eisopcfKeySuppressesAtAnyDeclaration() {
        // Class level: whole class suppressed.
        twoCheckerHelper()
                .expectNoDiagnostics()
                .addSourceLines(
                        "test/SuppressedClass.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "@SuppressWarnings(\"eisopcf\")",
                        "class SuppressedClass {",
                        "  int m(@Nullable String s, Object a, Object b) {",
                        "    int len = s.length();",
                        "    boolean eq = a == b;",
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        "}")
                .doTest();

        // Method level: the annotated method is suppressed, but a sibling method still reports.
        twoCheckerHelper()
                .addSourceLines(
                        "test/MethodEisopcf.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class MethodEisopcf {",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  int suppressed(@Nullable String s, Object a, Object b) {",
                        "    int len = s.length();",
                        "    boolean eq = a == b;",
                        "    return len + (eq ? 1 : 0);",
                        "  }",
                        "  int reported(@Nullable String s) {",
                        "    // BUG: Diagnostic contains: dereference.of.nullable",
                        "    return s.length();",
                        "  }",
                        "}")
                .doTest();

        // Field level: a finding in an annotated field's initializer is suppressed, but a finding
        // in a sibling (unannotated) field's initializer still reports.
        twoCheckerHelper()
                .addSourceLines(
                        "test/FieldEisopcf.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class FieldEisopcf {",
                        "  static @Nullable String nullable() { return null; }",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  static int lenA = nullable().length();", // suppressed: field is
                        // annotated
                        "  // BUG: Diagnostic contains: dereference.of.nullable",
                        "  static int lenB = nullable().length();", // still reported
                        "}")
                .doTest();

        // Local-variable level: only the finding on the annotated declaration is suppressed; a
        // finding in another statement of the same method still reports. This is the fine-grained
        // "extract to a local variable and suppress just that declaration" pattern.
        twoCheckerHelper()
                .addSourceLines(
                        "test/LocalVarEisopcf.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class LocalVarEisopcf {",
                        "  int m(@Nullable String s, @Nullable String t) {",
                        "    @SuppressWarnings(\"eisopcf\")",
                        "    int lenS = s.length();", // suppressed: this declaration is annotated
                        "    // BUG: Diagnostic contains: dereference.of.nullable",
                        "    int lenT = t.length();", // still reported
                        "    return lenS + lenT;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * Checker Framework options are passed exactly as in standalone mode: with javac {@code -A}
     * options, which reach the checkers through {@code processingEnv.getOptions()} (Error Prone
     * runs with annotation processing enabled, so javac records {@code -A} options even though no
     * Checker Framework annotation processor is registered). This covers both the common {@link
     * org.checkerframework.framework.source.SourceChecker} options (e.g. {@code -Astubs=...}) and
     * checker-specific options (written {@code -ACheckerName_option=...}).
     *
     * <p>Here the common {@code -AsuppressWarnings=nullness} option suppresses the Nullness Checker
     * finding that {@link #nullnessCheckerReportsBadReturn()} otherwise reports, demonstrating that
     * a {@code -A} option changes checker behavior under the {@code eisopcf} plugin.
     */
    @Test
    public void checkerOptionsArePassedWithDashA() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        CompilationTestHelper optionHelper =
                CompilationTestHelper.newInstance(scanner, getClass())
                        .setArgs(
                                append(
                                        BASE_ARGS,
                                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                                        // A standard SourceChecker option, passed with -A as in
                                        // standalone mode; suppresses all "nullness"-prefixed
                                        // findings.
                                        "-AsuppressWarnings=nullness"));
        optionHelper
                .expectNoDiagnostics()
                .addSourceLines(
                        "test/WithOption.java",
                        "package test;",
                        "class WithOption {",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }
}
