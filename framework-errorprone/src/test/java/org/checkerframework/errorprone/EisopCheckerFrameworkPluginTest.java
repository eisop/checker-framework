package org.checkerframework.errorprone;

import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.bugpatterns.SelfAssignment;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.main.Main.Result;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

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

    /** Fully-qualified name of the Lock Checker (lives in the :checker module, test-only dep). */
    private static final String LOCK_CHECKER = "org.checkerframework.checker.lock.LockChecker";

    /**
     * javac flags Error Prone requires. The --add-exports/--add-opens needed to run in-process are
     * supplied as JVM args by the module's build.gradle (they have no effect as compiler args). The
     * Checker Framework option {@code noJreVersionCheck} suppresses the Checker Framework's own
     * NOTE about untested JRE versions, so that tests asserting an exact diagnostic count do not
     * depend on which JDK runs the build (CI covers JDKs the Checker Framework does not consider
     * "tested" yet, e.g. 24).
     */
    private static final List<String> BASE_ARGS =
            Arrays.asList(
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true",
                    "-XepOpt:eisopcf:noJreVersionCheck");

    /** A helper running the {@code eisopcf} plugin with the Nullness Checker selected. */
    private CompilationTestHelper helper;

    /** Holds the stub file that {@link #checkerOptionsArePassedAsErrorProneFlags} writes. */
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
     * confirming the reflective checker resolution surfaces mistakes. Like an empty selection, it
     * fails the compilation whatever {@code -Xep:eisopcf:} severity is in effect.
     */
    @Test
    public void unknownCheckerNameIsReported() {
        // The diagnostic carries no source position, so it cannot be anchored with a
        // "// BUG: Diagnostic contains:" marker; assert instead that the compilation fails.
        helperWith("-XepOpt:eisopcf:checkers=com.example.NoSuchChecker")
                .expectResult(Result.ERROR)
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
                        "class Any {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }

    /**
     * Both configuration errors fail the compilation even at the check's default WARNING severity,
     * since neither is a finding whose importance that severity should scale.
     */
    @Test
    public void configurationErrorsAreUnconditional() {
        helperWith("-XepOpt:eisopcf:checkers=com.example.NoSuchChecker", "-Xep:eisopcf:WARN")
                .expectResult(Result.ERROR)
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
                        "class Any {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }

    /**
     * Enabling the check without selecting a checker is reported, rather than silently doing
     * nothing. The dangerous outcome is not a missing feature but a build that looks like it is
     * type-checking and is not, so this is treated as a configuration error, and reported the same
     * unconditional way as the unresolvable checker name above.
     */
    @Test
    public void noCheckerSelectedIsReported() {
        CompilationTestHelper helper =
                CompilationTestHelper.newInstance(
                                EisopCheckerFrameworkPlugin.class,
                                EisopCheckerFrameworkPluginTest.class)
                        .setArgs(BASE_ARGS);
        // The diagnostic carries no source position, so it cannot be anchored with a
        // "// BUG: Diagnostic contains:" marker; assert instead that the compilation fails, which
        // is the property that distinguishes this from the warning it used to be.
        helper.expectResult(Result.ERROR)
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
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
     * Under the plugin, Checker Framework options are passed only as Error Prone flags (see {@link
     * #checkerOptionsArePassedAsErrorProneFlags()}); a javac {@code -A} option is not read. Javac
     * does not warn about an unused {@code -A} option when no annotation processor runs, so a
     * {@code -A} option that a selected checker supports would be silently ignored; the plugin
     * reports it as a configuration error instead, naming the flag to use.
     */
    @Test
    public void dashAOptionOfASelectedCheckerIsReported() {
        // The diagnostic carries no source position, so it cannot be anchored with a
        // "// BUG: Diagnostic contains:" marker; assert instead that the compilation fails.
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER, "-AsuppressWarnings=nullness")
                .expectResult(Result.ERROR)
                .addSourceLines(
                        "test/WithOption.java",
                        "package test;",
                        "class WithOption {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }

    /**
     * A {@code -A} option that no selected checker supports may belong to another annotation
     * processor of the compilation, so it is left alone: the compilation succeeds and the checker
     * runs as usual.
     */
    @Test
    public void unrelatedDashAOptionIsIgnored() {
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER, "-Acom.example.other=1")
                .expectResult(Result.OK)
                .addSourceLines(
                        "test/Unrelated.java",
                        "package test;",
                        "class Unrelated {",
                        "  // BUG: Diagnostic contains: return.type.incompatible",
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
        helperWith(
                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                        "-XepOpt:eisopcf:warnUnneededSuppressions")
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

    /**
     * A conflicting pair of declaration annotations on a package declaration is reported. A {@code
     * package-info.java} declares no class, so Error Prone's scanner never offers a {@code
     * ClassTree} for it and {@code matchClass} cannot reach it; the plugin drives the package
     * declaration from {@code matchCompilationUnit} instead.
     */
    @Test
    public void conflictingAnnotationsOnAPackageAreReported() {
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER)
                .addSourceLines(
                        "demo/package-info.java",
                        "@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)",
                        "@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.FIELD)",
                        "// BUG: Diagnostic contains: conflicting.defaults",
                        "package demo;",
                        "",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "import org.checkerframework.framework.qual.DefaultQualifier;",
                        "import org.checkerframework.framework.qual.TypeUseLocation;")
                .doTest();
    }

    /**
     * Control for {@link #conflictingAnnotationsOnAPackageAreReported}: a package declaration whose
     * annotations do not conflict is accepted, so the new dispatch does not report on every {@code
     * package-info.java} it now reaches.
     */
    @Test
    public void aConsistentPackageDeclarationIsAccepted() {
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER)
                .addSourceLines(
                        "demo/package-info.java",
                        "@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)",
                        "package demo;",
                        "",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "import org.checkerframework.framework.qual.DefaultQualifier;",
                        "import org.checkerframework.framework.qual.TypeUseLocation;")
                .doTest();
    }

    /**
     * Returns the source lines of a class whose two {@code @DefaultQualifier} annotations conflict,
     * so that the Checker Framework reports {@code conflicting.defaults} on the class's element
     * rather than on a tree. The lines carry a {@code // BUG:} marker for that finding.
     *
     * @param className the simple name of the class, in package {@code demo}
     * @return the source lines
     */
    private static String[] classWithConflictingDefaults(String className) {
        return new String[] {
            "package demo;",
            "import org.checkerframework.checker.nullness.qual.NonNull;",
            "import org.checkerframework.checker.nullness.qual.Nullable;",
            "import org.checkerframework.framework.qual.DefaultQualifier;",
            "import org.checkerframework.framework.qual.TypeUseLocation;",
            "@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)",
            "@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.FIELD)",
            "// BUG: Diagnostic contains: conflicting.defaults",
            "public class " + className + " {}"
        };
    }

    /**
     * A finding the Checker Framework reports on an element (here {@code conflicting.defaults} on a
     * class) is an {@code eisopcf} finding at the check's severity, like one reported on a tree.
     * The check's default severity is WARNING, so the compilation succeeds; printed through javac's
     * {@code Messager} instead, the finding would be an error whatever the severity.
     */
    @Test
    public void elementFindingHonorsSeverity() {
        helper.expectResult(Result.OK)
                .addSourceLines(
                        "demo/Conflicting.java", classWithConflictingDefaults("Conflicting"))
                .doTest();
    }

    /**
     * Like {@link #elementFindingHonorsSeverity}, for an element finding on a package declaration,
     * which the plugin reaches through {@code matchCompilationUnit} rather than {@code matchClass}.
     */
    @Test
    public void packageElementFindingHonorsSeverity() {
        helper.expectResult(Result.OK)
                .addSourceLines(
                        "demo/package-info.java",
                        "@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)",
                        "@DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.FIELD)",
                        "// BUG: Diagnostic contains: conflicting.defaults",
                        "package demo;",
                        "",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "import org.checkerframework.framework.qual.DefaultQualifier;",
                        "import org.checkerframework.framework.qual.TypeUseLocation;")
                .doTest();
    }

    /**
     * A finding reported on an element is suppressed by {@code @SuppressWarnings("eisopcf")} on
     * that element. The suppressed class is nested: Error Prone does not run the check at all on a
     * top-level class that suppresses it, so suppression there would hold whichever way the finding
     * is reported.
     */
    @Test
    public void elementFindingIsSuppressibleViaErrorProne() {
        helper.expectNoDiagnostics()
                .addSourceLines(
                        "demo/Outer.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "import org.checkerframework.framework.qual.DefaultQualifier;",
                        "import org.checkerframework.framework.qual.TypeUseLocation;",
                        "public class Outer {",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  @DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.FIELD)",
                        "  @DefaultQualifier(value = NonNull.class, locations = TypeUseLocation.FIELD)",
                        "  static class Inner {}",
                        "}")
                .doTest();
    }

    /**
     * A finding reported on an element does not stop Error Prone from analyzing the compilation
     * units that follow. Error Prone skips the remaining units once javac's error count exceeds the
     * errors it reported itself, so an element finding that bypassed it as a javac error would
     * leave the finding in {@code CSecond.java} unreported.
     */
    @Test
    public void elementFindingDoesNotStopLaterUnits() {
        helper.addSourceLines("demo/AFirst.java", classWithConflictingDefaults("AFirst"))
                .addSourceLines(
                        "demo/CSecond.java",
                        "package demo;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public class CSecond {",
                        "    @Nullable Object field;",
                        "    String m() {",
                        "        // BUG: Diagnostic contains: dereference.of.nullable",
                        "        return field.toString();",
                        "    }",
                        "}")
                .doTest();
    }

    /**
     * {@code inconsistent.constructor.type}, which the Lock Checker reports on a constructor's
     * element, is suppressed by {@code @SuppressWarnings("eisopcf")} on the constructor.
     */
    @Test
    public void constructorElementFindingIsSuppressibleViaErrorProne() {
        helperWith("-XepOpt:eisopcf:checkers=" + LOCK_CHECKER)
                .expectNoDiagnostics()
                .addSourceLines(
                        "demo/Unknown.java",
                        "package demo;",
                        "import org.checkerframework.checker.lock.qual.GuardedByUnknown;",
                        "public @GuardedByUnknown class Unknown {",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  Unknown() {}",
                        "}")
                .doTest();
    }

    /**
     * Control for {@link #constructorElementFindingIsSuppressibleViaErrorProne}: without the
     * suppression, the finding is reported.
     */
    @Test
    public void constructorElementFindingIsReported() {
        helperWith("-XepOpt:eisopcf:checkers=" + LOCK_CHECKER)
                .addSourceLines(
                        "demo/Unknown.java",
                        "package demo;",
                        "import org.checkerframework.checker.lock.qual.GuardedByUnknown;",
                        "public @GuardedByUnknown class Unknown {",
                        "  // BUG: Diagnostic contains: inconsistent.constructor.type",
                        "  Unknown() {}",
                        "}")
                .doTest();
    }

    /**
     * Writes a stub file that makes {@code org.junit.runner.Description.getMethodName()} (an
     * unannotated library method, which the Nullness Checker otherwise treats as returning {@code
     * NonNull}) return {@code @Nullable}.
     *
     * @return the stub file
     * @throws IOException if the file cannot be written
     */
    private File writeDescriptionStub() throws IOException {
        File stub = temporaryFolder.newFile("Description.astub");
        Files.write(
                stub.toPath(),
                Arrays.asList(
                        "package org.junit.runner;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "public final class Description {",
                        "  public @Nullable String getMethodName();",
                        "}"),
                StandardCharsets.UTF_8);
        return stub;
    }

    /**
     * Checker Framework options are passed as Error Prone flags, {@code
     * -XepOpt:eisopcf:<option>=<value>}, each read as the {@code -A<option>=<value>} form would be.
     * Several options are given in one compilation, and each has its own observable effect:
     *
     * <ul>
     *   <li>{@code stubs}, whose value is a file path: the stub makes {@code getMethodName()}
     *       nullable, so dereferencing its result is reported;
     *   <li>{@code warnUnneededSuppressions}, without a value: Error Prone gives such a flag the
     *       value {@code true}, which the checker (which only tests the option's presence) reads as
     *       {@code -AwarnUnneededSuppressions}, so the unneeded suppression is reported;
     *   <li>{@code suppressWarnings}, whose value contains commas and colons: exactly the two
     *       listed message keys are suppressed, while the dereference is still reported;
     *   <li>{@code NullnessChecker_lint}, a checker-prefixed option read through {@code
     *       getLintOption} by a subchecker (the Nullness Checker's visitor belongs to {@code
     *       NullnessNoInitSubchecker}): the redundant null comparison is reported.
     * </ul>
     *
     * @throws IOException if the stub file cannot be written
     */
    @Test
    public void checkerOptionsArePassedAsErrorProneFlags() throws IOException {
        helperWith(
                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                        "-XepOpt:eisopcf:stubs=" + writeDescriptionStub().getAbsolutePath(),
                        "-XepOpt:eisopcf:warnUnneededSuppressions",
                        "-XepOpt:eisopcf:suppressWarnings=nullness:return.type.incompatible,"
                                + "nullness:assignment.type.incompatible",
                        "-XepOpt:eisopcf:NullnessChecker_lint=redundantNullComparison")
                .addSourceLines(
                        "test/Options.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.junit.runner.Description;",
                        "class Options {",
                        "  String field = null;", // assignment.type.incompatible, suppressed
                        "  String ret() { return null; }", // return.type.incompatible, suppressed
                        "  int stubbed(Description d) {",
                        "    // BUG: Diagnostic contains: dereference.of.nullable",
                        "    return d.getMethodName().length();",
                        "  }",
                        "  // BUG: Diagnostic contains: unneeded.suppression",
                        "  @SuppressWarnings(\"nullness\")",
                        "  String clean() { return \"x\"; }",
                        "  boolean lint(@NonNull String s) {",
                        "    // BUG: Diagnostic contains: nulltest.redundant",
                        "    return s == null;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * Control for {@link #checkerOptionsArePassedAsErrorProneFlags}: without the options, the
     * program reports exactly the two findings that the options there suppress, and none of the
     * three that the options there produce.
     */
    @Test
    public void withoutTheOptionsTheProgramReportsItsOwnFindings() {
        helper.addSourceLines(
                        "test/Options.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.junit.runner.Description;",
                        "class Options {",
                        "  // BUG: Diagnostic contains: assignment.type.incompatible",
                        "  String field = null;",
                        "  // BUG: Diagnostic contains: return.type.incompatible",
                        "  String ret() { return null; }",
                        "  int stubbed(Description d) {",
                        "    return d.getMethodName().length();",
                        "  }",
                        "  @SuppressWarnings(\"nullness\")",
                        "  String clean() { return \"x\"; }",
                        "  boolean lint(@NonNull String s) {",
                        "    return s == null;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * An {@code eisopcf:} flag that names an option no selected checker supports (here a misspelled
     * {@code stubs}) is a configuration error, like an unresolvable checker name: javac's "options
     * were not recognized by any processor" warning does not cover Error Prone flags, and an
     * ignored option would silently change what is checked.
     */
    @Test
    public void unsupportedCheckerOptionIsReported() {
        helperWith("-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER, "-XepOpt:eisopcf:stub=x.astub")
                .expectResult(Result.ERROR)
                .addSourceLines(
                        "test/Any.java",
                        "package test;",
                        "class Any {",
                        "  String m() { return \"x\"; }",
                        "}")
                .doTest();
    }

    /**
     * An annotation processor that does nothing. Running it makes javac warn about {@code -A}
     * options that no processor recognizes, as any real processor (AutoValue, Dagger, ...) does.
     */
    @SupportedAnnotationTypes("*")
    public static class NoOpProcessor extends AbstractProcessor {
        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
            return false;
        }
    }

    /**
     * The control for {@link #errorProneFlagsRaiseNoUnrecognizedOptionWarning()}: with {@link
     * NoOpProcessor} and {@code -Werror}, a {@code -A} option that no processor recognizes makes
     * javac warn and so fails the compilation. This is what a Checker Framework {@code -A} option
     * did under the plugin; the option here is one no checker supports, so that the plugin itself
     * does not report it.
     */
    @Test
    public void unrecognizedDashAOptionFailsWithAnotherProcessorAndWerror() {
        helperWith(
                        "-processor",
                        NoOpProcessor.class.getName(),
                        "-Werror",
                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                        "-XepOpt:eisopcf:suppressWarnings=nullness",
                        "-Acom.example.unrecognized=1")
                .expectResult(Result.ERROR)
                .addSourceLines(
                        "test/WithProcessor.java",
                        "package test;",
                        "class WithProcessor {",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }

    /**
     * Under Error Prone, the Checker Framework is not an annotation processor, so when the
     * compilation runs some other annotation processor, javac would warn that no processor
     * recognized a Checker Framework {@code -A} option, and {@code -Werror} would fail the
     * compilation. The Error Prone flags are not {@code -A} options, so a compilation that runs
     * {@link NoOpProcessor} with {@code -Werror} passes cleanly, and the options take effect (the
     * {@code noJreVersionCheck} flag of {@link #BASE_ARGS} suppresses the JRE-version note, and the
     * finding the program contains is suppressed).
     */
    @Test
    public void errorProneFlagsRaiseNoUnrecognizedOptionWarning() {
        helperWith(
                        "-processor",
                        NoOpProcessor.class.getName(),
                        "-Werror",
                        "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER,
                        "-XepOpt:eisopcf:suppressWarnings=nullness")
                .expectNoDiagnostics()
                .addSourceLines(
                        "test/WithProcessor.java",
                        "package test;",
                        "class WithProcessor {",
                        "  String m() { return null; }",
                        "}")
                .doTest();
    }
}
