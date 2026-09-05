package org.checkerframework.errorprone;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.SelfAssignment;
import com.google.errorprone.scanner.ScannerSupplier;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private static final List<String> BASE_ARGS =
            Arrays.asList(
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true");

    /** A helper running the {@code eisopcf} plugin with the Nullness Checker selected. */
    private CompilationTestHelper helper;

    /**
     * Sets up a {@link CompilationTestHelper} running the {@code eisopcf} plugin with the Nullness
     * Checker.
     */
    @Before
    public void setUp() {
        helper = helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER);
    }

    /**
     * Returns a {@link CompilationTestHelper} that runs the {@code eisopcf} plugin with {@link
     * #BASE_ARGS} plus the given arguments.
     *
     * @param extraArgs arguments to append to {@link #BASE_ARGS}
     * @return a helper configured with those arguments
     */
    private static CompilationTestHelper helperWith(String... extraArgs) {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        List<String> args = new ArrayList<>(BASE_ARGS);
        args.addAll(Arrays.asList(extraArgs));
        return CompilationTestHelper.newInstance(scanner, EisopCheckerFrameworkPluginTest.class)
                .setArgs(args);
    }

    /**
     * Returns a {@link CompilationTestHelper} running the {@code eisopcf} plugin alongside an
     * unrelated Error Prone check, both at ERROR severity.
     *
     * <p>Error Prone reports its findings as javac diagnostics, so an ERROR from any check raises
     * {@code Log.nerrors}. The Checker Framework skips a compilation unit whose processing began
     * after {@code Log.nerrors} rose, on the assumption that a javac error means an unreliable AST.
     * Under Error Prone that assumption does not hold: another check's finding is not a broken AST.
     *
     * @return a helper running eisopcf and SelfAssignment, both as errors
     */
    private static CompilationTestHelper helperWithUnrelatedErrorProneCheck() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(
                        EisopCheckerFrameworkPlugin.class, SelfAssignment.class);
        List<String> args = new ArrayList<>(BASE_ARGS);
        args.add("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER);
        args.add("-Xep:eisopcf:ERROR");
        args.add("-Xep:SelfAssignment:ERROR");
        return CompilationTestHelper.newInstance(scanner, EisopCheckerFrameworkPluginTest.class)
                .setArgs(args);
    }

    /**
     * An Error Prone finding from an unrelated check does not stop the Checker Framework from
     * checking the compilation units that follow it.
     *
     * <p>The file names matter: Error Prone and the Checker Framework see the compilation units in
     * order, so the file carrying the unrelated error has to be processed first for the effect to
     * appear.
     */
    @Test
    public void unrelatedErrorProneErrorDoesNotSuppressLaterChecking() {
        helperWithUnrelatedErrorProneCheck()
                .addSourceLines(
                        "AFirst.java",
                        "package demo;",
                        "public class AFirst {",
                        "    int f;",
                        "    void selfAssign() {",
                        "        // BUG: Diagnostic contains: SelfAssignment",
                        "        this.f = this.f;",
                        "    }",
                        "}")
                .addSourceLines(
                        "CSecond.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public class CSecond {",
                        "    @Nullable Object field;",
                        "    String m() {",
                        "        // BUG: Diagnostic contains: dereference.of.nullable",
                        "        return field.toString();",
                        "    }",
                        "}")
                .addSourceLines(
                        "DSecond.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public class DSecond {",
                        "    @Nullable Object field;",
                        "    String m() {",
                        "        // BUG: Diagnostic contains: dereference.of.nullable",
                        "        return field.toString();",
                        "    }",
                        "}")
                .doTest();
    }

    /**
     * Control for {@link #unrelatedErrorProneErrorDoesNotSuppressLaterChecking}: with no error in
     * the first compilation unit, the later units are checked. Isolates the effect of the earlier
     * error from any question of whether the plugin sees these classes at all.
     */
    @Test
    public void withoutAnEarlierErrorTheLaterUnitsAreChecked() {
        helperWithUnrelatedErrorProneCheck()
                .addSourceLines(
                        "AFirst.java",
                        "package demo;",
                        "public class AFirst {",
                        "    int f;",
                        "    void noSelfAssign() {",
                        "        this.f = this.f + 1;",
                        "    }",
                        "}")
                .addSourceLines(
                        "CSecond.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public class CSecond {",
                        "    @Nullable Object field;",
                        "    String m() {",
                        "        // BUG: Diagnostic contains: dereference.of.nullable",
                        "        return field.toString();",
                        "    }",
                        "}")
                .addSourceLines(
                        "DSecond.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public class DSecond {",
                        "    @Nullable Object field;",
                        "    String m() {",
                        "        // BUG: Diagnostic contains: dereference.of.nullable",
                        "        return field.toString();",
                        "    }",
                        "}")
                .doTest();
    }

    /**
     * Returns a {@link CompilationTestHelper} with both the Nullness and Interning checkers
     * selected.
     *
     * @return a helper running both checkers
     */
    private static CompilationTestHelper twoCheckerHelper() {
        return helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER + "," + INTERNING_CHECKER);
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
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER, "-Xep:eisopcf:ERROR")
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
        twoCheckerHelper()
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
        helperWith("-XepOpt:eisopcf:checkers=com.example.NoSuchChecker")
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
                        "// BUG: Diagnostic contains: Checker class not found",
                        "class Any {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
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
        helperWith(
                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                        // A standard SourceChecker option, passed with -A as in standalone mode;
                        // suppresses all "nullness"-prefixed findings.
                        "-AsuppressWarnings=nullness")
                .expectNoDiagnostics()
                .addSourceLines(
                        "test/WithOption.java",
                        "package test;",
                        "class WithOption {",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }

    /**
     * A finding produced at the end of type-checking a class, rather than at a tree the visitor is
     * on, still reaches Error Prone positioned at its own tree. {@code -AwarnUnneededSuppressions}
     * is the user-facing instance: the Checker Framework issues it from {@code
     * warnUnneededSuppressions()}, after the visitor has finished the class.
     */
    @Test
    public void unneededSuppressionWarningIsReported() {
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER, "-AwarnUnneededSuppressions")
                .addSourceLines(
                        "test/Unneeded.java",
                        "package test;",
                        "class Unneeded {",
                        "  // BUG: Diagnostic contains: unneeded.suppression",
                        "  @SuppressWarnings(\"nullness\")",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }

    /**
     * A finding inside a nested class is reported exactly once. Error Prone's scanner matches every
     * class node (including nested ones), but the Checker Framework already type-checks a nested
     * class as part of its enclosing top-level class's recursive scan; driving it again per nested
     * class reported each such finding once per level of nesting. Regression test for that
     * double-reporting bug: the {@code // BUG:} marker asserts the finding is present, and
     * expecting no other diagnostics asserts it is not duplicated.
     */
    @Test
    public void findingInNestedClassReportedOnce() {
        helper.addSourceLines(
                        "test/Nested.java",
                        "package test;",
                        "class Nested {",
                        "  class Inner {",
                        "    // BUG: Diagnostic contains: return.type.incompatible",
                        "    String m() { return null; }",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * A finding inside a local class (nested two levels deep, within a method) is reported exactly
     * once. Deeper nesting compounded the double-reporting bug (n+1 reports for depth n), and a
     * local class is a non-{@code TOP_LEVEL} nesting kind distinct from a member class, so this
     * covers that the top-level-only driving handles it too.
     */
    @Test
    public void findingInLocalClassReportedOnce() {
        helper.addSourceLines(
                        "test/WithLocal.java",
                        "package test;",
                        "class WithLocal {",
                        "  void outer() {",
                        "    class Local {",
                        "      // BUG: Diagnostic contains: return.type.incompatible",
                        "      String m() { return null; }",
                        "    }",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * A finding inside an anonymous class is reported exactly once. An anonymous class is a
     * non-{@code TOP_LEVEL} nesting kind ({@code ANONYMOUS}), distinct from member and local
     * classes; this confirms the top-level-only driving handles it too, so the finding is not
     * duplicated.
     */
    @Test
    public void findingInAnonymousClassReportedOnce() {
        helper.addSourceLines(
                        "test/WithAnon.java",
                        "package test;",
                        "import java.util.concurrent.Callable;",
                        "class WithAnon {",
                        "  Callable<String> c =",
                        "      new Callable<String>() {",
                        "        @Override",
                        "        // BUG: Diagnostic contains: return.type.incompatible",
                        "        public String call() { return null; }",
                        "      };",
                        "}")
                .doTest();
    }
}
