package org.checkerframework.errorprone;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.Context;

import org.checkerframework.framework.source.DiagnosticSink;
import org.checkerframework.framework.source.SuggestedFixData;

import javax.inject.Inject;
import javax.tools.Diagnostic;

/**
 * Umbrella Error Prone check that runs the EISOP Checker Framework as an Error Prone plugin.
 *
 * <p>This is a single entry point: it builds one AST/CFG per compilation and runs the Checker
 * Framework type system(s) selected by the {@code eisopcf:checkers} Error Prone option over it,
 * rather than registering multiple independent annotation processors. It mirrors the Checker
 * Framework's own per-class ({@link ClassTree}) processing granularity by implementing {@link
 * ClassTreeMatcher}.
 *
 * <p>Selection example:
 *
 * <pre>{@code
 * -XepOpt:eisopcf:checkers=org.checkerframework.checker.nullness.NullnessChecker
 * }</pre>
 *
 * <p>The Error Prone option namespace is {@code eisopcf}, chosen to keep the EISOP Checker
 * Framework distinct from the typetools Checker Framework. Multiple checkers may be given as a
 * comma-separated list (see {@link #CHECKERS_FLAG}); running several type systems over one shared
 * AST/CFG is completed in a later task.
 *
 * <p>Error Prone discovers this plugin through the {@code ServiceLoader} registration in {@code
 * META-INF/services/com.google.errorprone.bugpatterns.BugChecker} (a hand-written resource rather
 * than {@code @AutoService}; see the architectural decision log for why).
 *
 * <p><b>Diagnostics.</b> Checker Framework findings are reported as Error Prone {@code
 * Description}s for this check ({@code eisopcf}) via a {@link DiagnosticSink} installed on the
 * driver, so Error Prone severity and {@code @SuppressWarnings("eisopcf")} suppression apply.
 * {@link #matchClass} therefore reports through {@code state.reportMatch} and returns {@link
 * Description#NO_MATCH}.
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

    /**
     * Error Prone option naming the Checker Framework checker(s) to run: a comma-separated list of
     * fully-qualified {@code SourceChecker} class names. Used as {@code
     * -XepOpt:eisopcf:checkers=<fully.qualified.CheckerClass>[,<...>]}.
     */
    public static final String CHECKERS_FLAG = "eisopcf:checkers";

    /**
     * The comma-separated list of fully-qualified checker class names, from {@link #CHECKERS_FLAG}.
     * Resolved to actual checkers lazily, once per compilation (see {@link #driverFor}).
     */
    private final ImmutableList<String> checkerClassNames;

    /**
     * The driver for the current compilation, created lazily on the first {@link #matchClass}.
     * Error Prone reuses a single {@code BugChecker} instance across the classes of one
     * compilation, but to be robust against instance reuse across compilations this is validated
     * against the compilation {@link Context} in {@link #driverFor}.
     */
    private transient CheckerFrameworkDriver driver;

    /** The context the current {@link #driver} was created for. */
    private transient Context driverContext;

    /**
     * The {@link VisitorState} of the in-progress {@link #matchClass} call, made available to the
     * {@link #diagnosticSink()} (which fires re-entrantly while the driver type-checks the class).
     * Not part of any persistent state.
     */
    private transient VisitorState currentState;

    /**
     * Whether a configuration error (e.g. an unresolvable checker name) has already been reported,
     * so it is surfaced once per compilation rather than on every class.
     */
    private transient boolean configErrorReported = false;

    /**
     * Constructs the plugin with no configuration. Error Prone uses this when instantiating checks
     * without flags; no checkers will be selected, so the plugin is inert.
     */
    public EisopCheckerFrameworkPlugin() {
        this.checkerClassNames = ImmutableList.of();
    }

    /**
     * Constructs the plugin from Error Prone flags, reading {@link #CHECKERS_FLAG}.
     *
     * @param flags the Error Prone flags for this compilation
     */
    @Inject
    public EisopCheckerFrameworkPlugin(ErrorProneFlags flags) {
        this.checkerClassNames = flags.getListOrEmpty(CHECKERS_FLAG);
    }

    /**
     * Returns the driver for the given compilation context, creating and initializing it on first
     * use. Also registers a one-time {@link TaskListener} that calls {@link
     * CheckerFrameworkDriver#finish()} when the compilation ends, so the Checker Framework's
     * end-of-compilation processing (e.g. unneeded-suppression warnings) runs.
     *
     * @param context the compilation context
     * @return the initialized driver, or {@code null} if no checkers are selected
     */
    private CheckerFrameworkDriver driverFor(Context context) {
        if (checkerClassNames.isEmpty()) {
            return null;
        }
        if (driver != null && driverContext == context) {
            return driver;
        }
        CheckerFrameworkDriver newDriver =
                CheckerFrameworkDriver.create(context, checkerClassNames, diagnosticSink());
        this.driver = newDriver;
        this.driverContext = context;
        // Run end-of-compilation processing when javac finishes.  A CompilationUnitTree-scoped
        // event is not sufficient (the CF's typeProcessingOver is a whole-compilation step), so
        // register a listener that fires finish() once, at the last COMPILATION-finished event.
        MultiTaskListener.instance(context)
                .add(
                        new TaskListener() {
                            @Override
                            public void finished(TaskEvent e) {
                                if (e.getKind() == TaskEvent.Kind.COMPILATION) {
                                    newDriver.finish();
                                }
                            }
                        });
        return newDriver;
    }

    /**
     * Builds the {@link DiagnosticSink} that turns Checker Framework findings into Error Prone
     * {@link Description}s and reports them through the {@link VisitorState} active during the
     * current {@link #matchClass} call.
     *
     * <p>Each finding becomes a {@code Description} for this check ({@code eisopcf}), positioned at
     * the finding's source tree and carrying the Checker Framework's formatted message. Error Prone
     * then applies this check's configured severity and its suppression logic.
     *
     * <p>Severity/suppression notes:
     *
     * <ul>
     *   <li>Error Prone severity is per-<em>check</em>, so all findings share the {@code eisopcf}
     *       severity (default WARNING, overridable via {@code -Xep:eisopcf:ERROR}); the Checker
     *       Framework diagnostic kind (error vs. warning) is preserved textually in the message.
     *   <li>Suppression via {@code @SuppressWarnings("eisopcf")} is honored at the granularity of
     *       the enclosing class (the tree the plugin matches). Finer-grained Checker Framework
     *       suppression strings still work through the Checker Framework's own mechanism.
     * </ul>
     *
     * @return the diagnostic sink
     */
    private DiagnosticSink diagnosticSink() {
        return new DiagnosticSink() {
            @Override
            public void report(
                    Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root) {
                reportWithFix(kind, message, source, root, null);
            }

            @Override
            public void reportWithFix(
                    Diagnostic.Kind kind,
                    String message,
                    Tree source,
                    CompilationUnitTree root,
                    SuggestedFixData fix) {
                VisitorState base = currentState;
                if (base == null) {
                    // Should not happen: findings are produced only while matchClass is running.
                    return;
                }
                // Anchor the finding (and its suppression fix) at the finding's own source tree,
                // not the enclosing class that matchClass is visiting.  This makes
                // @SuppressWarnings land on the nearest suppressible element to the finding (e.g.
                // the enclosing method), matching how the finding is located.
                VisitorState state = base;
                Tree position;
                if (source != null) {
                    TreePath findingPath = TreePath.getPath(root, source);
                    if (findingPath != null) {
                        state = base.withPath(findingPath);
                    }
                    position = source;
                } else {
                    position = base.getPath().getLeaf();
                }
                Description.Builder builder =
                        buildDescription(position).setMessage(formatMessage(kind, message));
                // Always offer a suppression fix, mirroring how Error Prone's own checks let users
                // add @SuppressWarnings via the patch pipeline.
                builder.addFix(SuggestedFixes.addSuppressWarnings(state, canonicalName()));
                // If the Checker Framework supplied a machine-applicable fix, translate it too.
                if (fix != null) {
                    builder.addFix(toErrorProneFix(fix));
                }
                state.reportMatch(builder.build());
            }
        };
    }

    /**
     * Translates a Checker-Framework-neutral {@link SuggestedFixData} into an Error Prone {@link
     * com.google.errorprone.fixes.SuggestedFix}. This is the boundary at which the neutral
     * source-offset replacements become an Error Prone fix; the Checker Framework core never
     * references Error Prone types.
     *
     * @param fix the neutral fix data
     * @return the equivalent Error Prone fix
     */
    private static SuggestedFix toErrorProneFix(SuggestedFixData fix) {
        SuggestedFix.Builder builder = SuggestedFix.builder();
        for (SuggestedFixData.Replacement r : fix.getReplacements()) {
            builder.replace(r.startPosition, r.endPosition, r.text);
        }
        return builder.build();
    }

    /**
     * Formats a finding's message, prefixing warnings so the Checker Framework diagnostic kind is
     * not lost (Error Prone severity is per-check, not per-finding).
     *
     * @param kind the Checker Framework diagnostic kind
     * @param message the Checker Framework message text
     * @return the message to attach to the Error Prone {@code Description}
     */
    private static String formatMessage(Diagnostic.Kind kind, String message) {
        if (kind == Diagnostic.Kind.WARNING || kind == Diagnostic.Kind.MANDATORY_WARNING) {
            return "[warning] " + message;
        }
        return message;
    }

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        CheckerFrameworkDriver currentDriver;
        try {
            currentDriver = driverFor(state.context);
        } catch (IllegalArgumentException e) {
            // A configuration error (e.g. an unresolvable checker name).  Report it once, as an
            // eisopcf diagnostic, rather than throwing an unhandled plugin exception on every
            // class.
            if (!configErrorReported) {
                configErrorReported = true;
                return buildDescription(tree).setMessage(e.getMessage()).build();
            }
            return Description.NO_MATCH;
        }
        if (currentDriver == null) {
            // No checkers selected: the plugin is inert.
            return Description.NO_MATCH;
        }
        ClassSymbol classSymbol = ASTHelpers.getSymbol(tree);
        TreePath path = state.getPath();
        if (classSymbol == null || path == null) {
            return Description.NO_MATCH;
        }
        // Make the current state available to the diagnostic sink, which fires (re-entrantly) while
        // the driver processes this class.  Restore the previous value afterward for safety.
        VisitorState previous = currentState;
        currentState = state;
        try {
            currentDriver.process(classSymbol, path);
        } finally {
            currentState = previous;
        }
        // Findings are reported via state.reportMatch through the diagnostic sink, so there is no
        // Description to return here.
        return Description.NO_MATCH;
    }
}
