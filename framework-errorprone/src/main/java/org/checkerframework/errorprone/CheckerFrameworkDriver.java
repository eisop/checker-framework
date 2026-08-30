package org.checkerframework.errorprone;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

import org.checkerframework.framework.source.DiagnosticSink;
import org.checkerframework.framework.source.SourceChecker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * Mode-agnostic driver that runs one or more Checker Framework {@link SourceChecker}s, initialized
 * from a javac {@link Context}, over individual classes.
 *
 * <p>This is the bridge between Error Prone's per-class callbacks and the Checker Framework's
 * externally-driven type-processing lifecycle (see {@code AbstractTypeProcessor} / {@code
 * SourceChecker}). One driver corresponds to one compilation: it initializes the selected
 * checker(s) once (lazily, on first use), drives {@link SourceChecker#typeProcessExternally} per
 * class, and signals completion with {@link SourceChecker#typeProcessingOverExternally}.
 *
 * <p>The class of each checker is provided as a fully-qualified name and instantiated reflectively,
 * so this module needs no compile-time dependency on the module that contains the concrete checker
 * (e.g. the Nullness Checker lives in the {@code :checker} module, above this one in the dependency
 * graph).
 *
 * <p>This class references only the Checker Framework and the JDK; it contains no Error Prone
 * types.
 *
 * <p><b>Diagnostics.</b> An optional {@link DiagnosticSink} may be supplied to {@link #create};
 * when present, it is installed on every checker so that findings are delivered to the host (and,
 * for the Error Prone plugin, turned into Error Prone {@code Description}s). When absent, the
 * Checker Framework reports through its own {@code Messager}/{@code Trees} machinery, exactly as in
 * standalone mode.
 */
public final class CheckerFrameworkDriver {

    /** The initialized checkers to run, in order. */
    private final List<SourceChecker> checkers;

    /** True once {@link #finish()} has run, to make it idempotent. */
    private boolean finished = false;

    /**
     * Creates a driver over already-initialized checkers; use {@link #create}.
     *
     * @param checkers the initialized checkers to run, in order
     */
    private CheckerFrameworkDriver(List<SourceChecker> checkers) {
        this.checkers = checkers;
    }

    /**
     * Creates and initializes a driver for the given checkers over the given compilation context.
     *
     * @param context the javac context for the current compilation (e.g. Error Prone's {@code
     *     VisitorState.context})
     * @param checkerClassNames the fully-qualified names of the {@link SourceChecker} subclasses to
     *     run
     * @param sink an optional destination for findings; if non-null it is installed on every
     *     checker (via {@link SourceChecker#setDiagnosticSink}), so findings go to the host instead
     *     of javac. Pass {@code null} for Checker-Framework-native reporting.
     * @return an initialized driver
     * @throws IllegalArgumentException if {@code checkerClassNames} is empty, or a name does not
     *     resolve to an instantiable {@link SourceChecker}
     */
    public static CheckerFrameworkDriver create(
            Context context, List<String> checkerClassNames, DiagnosticSink sink) {
        if (checkerClassNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "No Checker Framework checkers selected; name at least one SourceChecker"
                            + " subclass.");
        }
        ProcessingEnvironment procEnv = EisopContextAdapter.getProcessingEnvironment(context);
        List<SourceChecker> initialized = new ArrayList<>(checkerClassNames.size());
        for (String className : checkerClassNames) {
            SourceChecker checker = instantiate(className);
            // Externally-driven: Error Prone owns the compilation TaskListener, so the checker must
            // not register its own AttributionTaskListener.  Must be set before init().
            checker.enableExternallyDrivenMode(true);
            checker.init(procEnv);
            // After init(), so that any diagnostic init() itself issues still goes to javac: the
            // host's sink is only usable once the host is processing a class.
            if (sink != null) {
                checker.setDiagnosticSink(sink);
            }
            initialized.add(checker);
        }
        return new CheckerFrameworkDriver(initialized);
    }

    /**
     * Instantiates a {@link SourceChecker} subclass by fully-qualified name using its no-argument
     * constructor.
     *
     * @param className the fully-qualified class name
     * @return a new instance
     * @throws IllegalArgumentException if the class cannot be found, is not a {@link
     *     SourceChecker}, or cannot be instantiated
     */
    private static SourceChecker instantiate(String className) {
        Class<?> clazz = loadCheckerClass(className);
        if (!SourceChecker.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(
                    className
                            + " is not a "
                            + SourceChecker.class.getName()
                            + "; the eisopcf:checkers option must name Checker Framework checkers.");
        }
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            return (SourceChecker) instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Could not instantiate checker " + className + " via its no-arg constructor.",
                    e);
        }
    }

    /**
     * Loads a checker class by name, trying several classloaders. Concrete checkers live in modules
     * above this one (e.g. the Nullness Checker in {@code checker.jar}); depending on how Error
     * Prone loaded this plugin (test classpath, processorpath via a {@code MaskedClassLoader}, or
     * an application classloader), the checker may be visible from a different loader than this
     * class's. In particular, the classloader that loaded the Checker Framework core ({@link
     * SourceChecker}) is the one that, in a valid deployment, also has the concrete checkers
     * alongside it.
     *
     * @param className the fully-qualified checker class name
     * @return the loaded class
     * @throws IllegalArgumentException if no candidate classloader can resolve the class
     */
    private static Class<?> loadCheckerClass(String className) {
        ClassLoader[] candidates = {
            // The loader that has the CF core, and (in a valid deployment) the checkers with it.
            SourceChecker.class.getClassLoader(),
            // The thread context classloader (set by build tools / launchers).
            Thread.currentThread().getContextClassLoader(),
            // This class's own loader.
            CheckerFrameworkDriver.class.getClassLoader(),
        };
        ClassNotFoundException last = null;
        for (ClassLoader loader : candidates) {
            if (loader == null) {
                continue;
            }
            try {
                return Class.forName(className, true, loader);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        throw new IllegalArgumentException(
                "Checker class not found on the classpath: "
                        + className
                        + ". Ensure the module containing the checker (e.g. checker.jar) is on the"
                        + " Error Prone processorpath (or classpath).",
                last);
    }

    /**
     * Runs all selected checkers over one class.
     *
     * @param element the type element of the class being processed
     * @param path the tree path to the class (leaf is a {@code ClassTree}), as the Checker
     *     Framework expects
     */
    public void process(TypeElement element, TreePath path) {
        for (SourceChecker checker : checkers) {
            checker.typeProcessExternally(element, path);
        }
    }

    /**
     * Signals that all classes have been processed. Invokes each checker's {@code
     * typeProcessingOverExternally} exactly once (idempotent).
     */
    public void finish() {
        if (finished) {
            return;
        }
        finished = true;
        for (SourceChecker checker : checkers) {
            checker.typeProcessingOverExternally();
        }
    }
}
