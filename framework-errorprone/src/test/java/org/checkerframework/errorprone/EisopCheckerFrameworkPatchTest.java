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
     * @param extraArgs additional javac or Error Prone flags, after {@link #ARGS}
     * @return the refactoring helper
     */
    private static BugCheckerRefactoringTestHelper refactoringHelper(String... extraArgs) {
        ScannerSupplier scanner =
                ScannerSupplier.fromBugCheckerClasses(EisopCheckerFrameworkPlugin.class);
        List<String> args = new ArrayList<>(ARGS);
        args.addAll(Arrays.asList(extraArgs));
        return BugCheckerRefactoringTestHelper.newInstance(
                        scanner, EisopCheckerFrameworkPatchTest.class)
                .setArgs(args.toArray(new String[0]));
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

    /**
     * The Nullness Checker reports {@code nullness.on.outer} for a nullness annotation written
     * before a qualified inner class type, where Java applies it to the outer type, and attaches a
     * fix that moves the annotation onto the inner type: {@code @Nullable Outer.Inner} becomes
     * {@code Outer.@Nullable Inner}.
     */
    @Test
    public void nullnessOnOuterMoveAnnotationFixIsApplied() {
        refactoringHelper()
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {}",
                        "  void m(@Nullable Outer.Inner p) {}",
                        "}")
                .addOutputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {}",
                        "  void m(Outer.@Nullable Inner p) {}",
                        "}")
                .doTest();
    }

    /**
     * The {@code nullness.on.outer} fix moves the annotation onto the innermost type of the
     * qualified name, also in a type argument, where javac attaches the annotation to the first
     * name rather than to the declaration, and leaves type arguments in place.
     */
    @Test
    public void nullnessOnOuterFixInTypeArgumentMovesToInnermostType() {
        refactoringHelper()
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Outer.java",
                        "package test;",
                        "import java.util.List;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {",
                        "    class Deep<T> {}",
                        "  }",
                        "  void m(List<@Nullable Outer.Inner.Deep<String>> p) {}",
                        "}")
                .addOutputLines(
                        "test/Outer.java",
                        "package test;",
                        "import java.util.List;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {",
                        "    class Deep<T> {}",
                        "  }",
                        "  void m(List<Outer.Inner.@Nullable Deep<String>> p) {}",
                        "}")
                .doTest();
    }

    /**
     * The {@code nullness.on.outer} fix copies the annotation as written, here with a
     * fully-qualified name, leaves other modifiers and array brackets in place, and also applies to
     * an annotation written between the names of the qualified type.
     */
    @Test
    public void nullnessOnOuterFixKeepsAnnotationSpellingModifiersAndArrayBrackets() {
        refactoringHelper()
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {",
                        "    class Deep {}",
                        "  }",
                        "  void m(final @org.checkerframework.checker.nullness.qual.Nullable"
                                + " Outer.Inner[] p) {}",
                        "  void n(Outer.@Nullable Inner.Deep p) {}",
                        "}")
                .addOutputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {",
                        "    class Deep {}",
                        "  }",
                        "  void m(final Outer.@org.checkerframework.checker.nullness.qual.Nullable"
                                + " Inner[] p) {}",
                        "  void n(Outer.Inner.@Nullable Deep p) {}",
                        "}")
                .doTest();
    }

    /**
     * When the inner type already carries a nullness annotation, the intended qualifier is unclear,
     * so {@code nullness.on.outer} carries no Checker Framework fix and the first fix is the
     * suppression fix.
     */
    @Test
    public void nullnessOnOuterOffersNoFixWhenInnerTypeIsAnnotated() {
        refactoringHelper()
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {}",
                        "  @Nullable Outer.@NonNull Inner m() {",
                        "    return new Inner();",
                        "  }",
                        "}")
                .addOutputLines(
                        "test/Outer.java",
                        "package test;",
                        "import org.checkerframework.checker.nullness.qual.NonNull;",
                        "import org.checkerframework.checker.nullness.qual.Nullable;",
                        "class Outer {",
                        "  class Inner {}",
                        "  @SuppressWarnings(\"eisopcf\")",
                        "  @Nullable Outer.@NonNull Inner m() {",
                        "    return new Inner();",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * Under {@code -AcheckPurityAnnotations}, a method annotated with both {@code @SideEffectFree}
     * and {@code @Deterministic} gets a {@code purity.effectively.pure} warning (reported by every
     * checker) with a fix that replaces the first of the two annotations by {@code @Pure} and
     * deletes the other. {@code Pure} is written by its simple name when the file imports it.
     */
    @Test
    public void purityEffectivelyPureFixReplacesBothAnnotationsByImportedPure() {
        refactoringHelper("-XepOpt:eisopcf:checkPurityAnnotations")
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/P.java",
                        "package test;",
                        "import org.checkerframework.dataflow.qual.Deterministic;",
                        "import org.checkerframework.dataflow.qual.Pure;",
                        "import org.checkerframework.dataflow.qual.SideEffectFree;",
                        "class P {",
                        "  @SideEffectFree",
                        "  @Deterministic",
                        "  int m() {",
                        "    return 1;",
                        "  }",
                        "}")
                .addOutputLines(
                        "test/P.java",
                        "package test;",
                        "import org.checkerframework.dataflow.qual.Deterministic;",
                        "import org.checkerframework.dataflow.qual.Pure;",
                        "import org.checkerframework.dataflow.qual.SideEffectFree;",
                        "class P {",
                        "  @Pure",
                        "  int m() {",
                        "    return 1;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * Without an import of {@code Pure}, the {@code purity.effectively.pure} fix writes its
     * fully-qualified name, so the result compiles; the annotations may be in either order and need
     * not be adjacent.
     */
    @Test
    public void purityEffectivelyPureFixUsesQualifiedNameWithoutImport() {
        refactoringHelper("-XepOpt:eisopcf:checkPurityAnnotations")
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/P.java",
                        "package test;",
                        "import org.checkerframework.dataflow.qual.Deterministic;",
                        "import org.checkerframework.dataflow.qual.SideEffectFree;",
                        "class P {",
                        "  @Deterministic public @SideEffectFree int m() {",
                        "    return 1;",
                        "  }",
                        "}")
                .addOutputLines(
                        "test/P.java",
                        "package test;",
                        "import org.checkerframework.dataflow.qual.Deterministic;",
                        "import org.checkerframework.dataflow.qual.SideEffectFree;",
                        "class P {",
                        "  @org.checkerframework.dataflow.qual.Pure public int m() {",
                        "    return 1;",
                        "  }",
                        "}")
                .doTest();
    }

    /**
     * A method with only one of the two annotations is not effectively pure: no finding, so no fix
     * and no change.
     */
    @Test
    public void purityWithSingleAnnotationIsUnchanged() {
        refactoringHelper("-XepOpt:eisopcf:checkPurityAnnotations")
                .setFixChooser(FixChoosers.FIRST)
                .addInputLines(
                        "test/P.java",
                        "package test;",
                        "import org.checkerframework.dataflow.qual.SideEffectFree;",
                        "class P {",
                        "  @SideEffectFree",
                        "  int m() {",
                        "    return 1;",
                        "  }",
                        "}")
                .expectUnchanged()
                .doTest();
    }
}
