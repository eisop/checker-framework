package org.checkerframework.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.scanner.ScannerSupplier;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Patch-mode tests for {@link EisopCheckerFrameworkPlugin}: exercising Error Prone's suggested-fix
 * / refactoring pipeline.
 *
 * <p>Every {@code eisopcf} finding carries an "add {@code @SuppressWarnings("eisopcf")}" fix, the
 * same way Error Prone's own checks offer a suppression fix. Applying that fix through Error
 * Prone's refactoring machinery ({@link BugCheckerRefactoringTestHelper}) rewrites the source,
 * which validates that Checker Framework findings reach the patch pipeline end-to-end.
 */
public class EisopCheckerFrameworkPatchTest {

    /** Fully-qualified name of the Nullness Checker (lives in the :checker module). */
    private static final String NULLNESS_CHECKER =
            "org.checkerframework.checker.nullness.NullnessChecker";

    /** javac flags Error Prone requires, plus the Nullness Checker selection. */
    private static final List<String> ARGS =
            Arrays.asList(
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true",
                    "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER);

    /**
     * Returns a refactoring helper that runs the {@code eisopcf} plugin with the Nullness Checker.
     *
     * @return the refactoring helper
     */
    private static BugCheckerRefactoringTestHelper refactoringHelper() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        return BugCheckerRefactoringTestHelper.newInstance(
                        scanner, EisopCheckerFrameworkPatchTest.class)
                .setArgs(ARGS.toArray(new String[0]));
    }

    /**
     * The suppression fix attached to a Nullness Checker finding is applied by Error Prone's patch
     * pipeline, inserting {@code @SuppressWarnings("eisopcf")} on the enclosing method.
     */
    @Test
    public void suppressionFixIsApplied() {
        refactoringHelper()
                // This finding carries no Checker Framework fix, so the suppression fix is first.
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Bad.java",
                        "package test;",
                        "class Bad {",
                        "  String m() {",
                        "    return null;",
                        "  }",
                        "}")
                .addOutputLines(
                        "test/Bad.java",
                        "package test;",
                        "class Bad {",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  String m() {",
                        "    return null;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * A Checker-Framework-supplied fix reaches Error Prone's patch pipeline. The Nullness Checker
     * reports {@code nullness.on.primitive} for a nullness annotation on a primitive type and
     * attaches a "remove the annotation" fix (via the framework-agnostic {@code SuggestedFixData}
     * channel). That fix is the first alternative (ahead of the always-present suppression fix), so
     * applying it rewrites {@code @Nullable int x} to {@code int x}.
     */
    @Test
    public void nullnessOnPrimitiveRemoveAnnotationFixIsApplied() {
        refactoringHelper()
                // The Checker Framework fix (remove the annotation) is the first fix.
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Prim.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Prim {",
                        "  @Nullable int x = 0;",
                        "}")
                .addOutputLines(
                        "test/Prim.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Prim {",
                        "  int x = 0;",
                        "}")
                .doTest();
    }
}
