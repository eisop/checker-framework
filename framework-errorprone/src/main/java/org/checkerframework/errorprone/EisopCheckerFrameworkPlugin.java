package org.checkerframework.errorprone;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.util.Context;

import javax.inject.Inject;

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
 * <p><b>Diagnostics (this task).</b> Findings are reported through the Checker Framework's own
 * {@code Messager}; Error Prone hosts the compilation ("2b" passthrough). Mapping findings to Error
 * Prone {@code Description}s (for Error Prone severity/suppression and the patch pipeline) is a
 * later task, which is why {@link #matchClass} returns {@link Description#NO_MATCH}.
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
                CheckerFrameworkDriver.create(context, checkerClassNames);
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

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        CheckerFrameworkDriver currentDriver = driverFor(state.context);
        if (currentDriver == null) {
            // No checkers selected: the plugin is inert.
            return Description.NO_MATCH;
        }
        ClassSymbol classSymbol = ASTHelpers.getSymbol(tree);
        TreePath path = state.getPath();
        if (classSymbol == null || path == null) {
            return Description.NO_MATCH;
        }
        currentDriver.process(classSymbol, path);
        // 2b passthrough: the Checker Framework reports via its own Messager, so there is no Error
        // Prone Description to return here.  (Task 5 changes this to emit EP Descriptions.)
        return Description.NO_MATCH;
    }
}
