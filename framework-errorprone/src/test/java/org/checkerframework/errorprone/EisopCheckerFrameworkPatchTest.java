package org.checkerframework.errorprone;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.FixChoosers;
import com.google.errorprone.scanner.ScannerSupplier;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Patch-mode tests for {@link EisopCheckerFrameworkPlugin}: exercising Error Prone's suggested-fix
 * / refactoring pipeline (goal 1c).
 *
 * <p>Every {@code eisopcf} finding carries an "add {@code @SuppressWarnings("eisopcf")}" fix, the
 * same way Error Prone's own checks offer a suppression fix. Applying that fix through Error
 * Prone's refactoring machinery ({@link BugCheckerRefactoringTestHelper}) rewrites the source,
 * which validates that Checker Framework findings reach the patch pipeline end-to-end.
 */
public class EisopCheckerFrameworkPatchTest {

    private static final String NULLNESS_CHECKER =
            "org.checkerframework.checker.nullness.NullnessChecker";

    private static final List<String> BASE_ARGS =
            Arrays.asList(
                    "-XDcompilePolicy=simple",
                    "--should-stop=ifError=FLOW",
                    "-XDaddTypeAnnotationsToSymbol=true");

    private static List<String> append(List<String> base, String... extra) {
        List<String> result = new ArrayList<>(base);
        result.addAll(Arrays.asList(extra));
        return result;
    }

    /**
     * The suppression fix attached to a Nullness Checker finding is applied by Error Prone's patch
     * pipeline, inserting {@code @SuppressWarnings("eisopcf")} on the enclosing method.
     */
    @Test
    public void suppressionFixIsApplied() {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        BugCheckerRefactoringTestHelper.newInstance(scanner, getClass())
                .setArgs(
                        append(BASE_ARGS, "-XepOpt:eisopcf:checkers=" + NULLNESS_CHECKER)
                                .toArray(new String[0]))
                // The suppression fix is the first fix attached to each finding.
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
}
