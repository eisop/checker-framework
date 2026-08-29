package org.checkerframework.errorprone;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.ClassTree;

/**
 * Umbrella Error Prone check that runs the EISOP Checker Framework as an Error Prone plugin.
 *
 * <p>This is a single entry point: it builds one AST/CFG per compilation and runs the CF type
 * system(s) selected by the {@code eisopcf} Error Prone option over it, rather than registering
 * multiple independent annotation processors. It mirrors the Checker Framework's own per-class
 * ({@link ClassTree}) processing granularity by implementing {@link ClassTreeMatcher}.
 *
 * <p>The Error Prone option namespace is {@code eisopcf} (e.g. {@code
 * -XepOpt:eisopcf:checkers=org.checkerframework.checker.nullness.NullnessChecker}), chosen to keep
 * the EISOP Checker Framework distinct from the typetools Checker Framework.
 *
 * <p>Error Prone discovers this plugin through the {@code ServiceLoader} registration in {@code
 * META-INF/services/com.google.errorprone.bugpatterns.BugChecker} (a hand-written resource rather
 * than {@code @AutoService}; see the architectural decision log for why).
 *
 * <p>This class is currently a scaffold: it registers with Error Prone and matches classes, but
 * does not yet run any checker. The driver wiring is added in a later task.
 */
@BugPattern(
        name = "eisopcf",
        summary = "EISOP Checker Framework type error.",
        // WARNING (not ERROR) as a conservative default while the bridge is under development; the
        // effective severity can be overridden through standard Error Prone configuration.
        severity = BugPattern.SeverityLevel.WARNING)
// The canonical check name is intentionally the short, user-facing token "eisopcf" (also the
// -XepOpt: option prefix and the @SuppressWarnings key), deliberately different from the
// descriptive class name; suppress Error Prone's BugPatternNaming check accordingly.
@SuppressWarnings("BugPatternNaming")
public class EisopCheckerFrameworkPlugin extends BugChecker implements ClassTreeMatcher {

    private static final long serialVersionUID = 1L;

    /** Creates the umbrella Error Prone check. */
    public EisopCheckerFrameworkPlugin() {}

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        // Scaffold only: real dispatch to the Checker Framework driver is added in a later task.
        return Description.NO_MATCH;
    }
}
