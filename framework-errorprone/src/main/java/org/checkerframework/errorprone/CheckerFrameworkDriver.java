package org.checkerframework.errorprone;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.util.Context;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.source.DiagnosticSink;
import org.checkerframework.framework.source.SourceChecker;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
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

    /**
     * True once {@link #finish()} has run. Makes {@link #finish()} idempotent at the driver level:
     * {@code SourceChecker.typeProcessingOverExternally} is already once-only per checker, but this
     * guard lets {@link #finish()} document and guarantee that contract on its own, without relying
     * on that internal detail, and short-circuits the per-checker loop on repeated calls (the
     * end-of-compilation TaskListener may fire {@code finish()} more than once).
     */
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
     * @param options Checker Framework options supplied by the host, as the option names that would
     *     follow {@code -A} (e.g. {@code stubs} or {@code NullnessChecker_stubs}) mapped to their
     *     values. Every checker reads them in place of the compilation's {@code -A} options,
     *     exactly as it would read those (see {@link SourceChecker#setHostOptions}). Each must be
     *     supported by at least one selected checker, as javac requires of a {@code -A} option.
     * @param sink an optional destination for findings; if non-null it is installed on every
     *     checker (via {@link SourceChecker#setDiagnosticSink}), so findings go to the host instead
     *     of javac. Pass {@code null} for Checker-Framework-native reporting.
     * @return an initialized driver
     * @throws IllegalArgumentException if {@code checkerClassNames} is empty, a name does not
     *     resolve to an instantiable {@link SourceChecker}, an option is not supported by any
     *     selected checker, or a {@code -A} option of the compilation is one that a selected
     *     checker supports (the checkers do not read {@code -A} options, so it would be silently
     *     ignored)
     */
    public static CheckerFrameworkDriver create(
            Context context,
            List<String> checkerClassNames,
            Map<String, String> options,
            @Nullable DiagnosticSink sink) {
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
            checker.setExternallyDriven(true);
            // Also before init(), which reads the options.
            checker.setHostOptions(options);
            checker.init(procEnv);
            // After init(), so that any diagnostic init() itself issues still goes to javac: the
            // host's sink is only usable once the host is processing a class.
            if (sink != null) {
                checker.setDiagnosticSink(sink);
            }
            initialized.add(checker);
        }
        checkOptionsAreSupported(options, initialized);
        checkNoDashAOptions(procEnv.getOptions(), initialized);
        return new CheckerFrameworkDriver(initialized);
    }

    /**
     * Checks that no {@code -A} option of the compilation is one that a selected checker supports.
     * The checkers read only the host's options (see {@link SourceChecker#setHostOptions}), and
     * javac does not warn about an unused {@code -A} option when no annotation processor runs, so
     * without this check a {@code -Astubs=...} written for standalone mode would be silently
     * ignored. A {@code -A} option that no selected checker supports is left alone: it may belong
     * to another annotation processor of the compilation.
     *
     * @param dashAOptions the {@code -A} options of the compilation, without the {@code -A}
     * @param checkers the initialized checkers
     * @throws IllegalArgumentException if a {@code -A} option is supported by a checker
     */
    private static void checkNoDashAOptions(
            Map<String, String> dashAOptions, List<SourceChecker> checkers) {
        StringJoiner replacements = new StringJoiner(", ");
        for (Map.Entry<String, String> option : dashAOptions.entrySet()) {
            String key = option.getKey();
            for (SourceChecker checker : checkers) {
                if (checker.getSupportedOptions().contains(key)) {
                    // javac records an option without a value, "-Afoo", as a null value.
                    String value = option.getValue();
                    String suffix = value == null ? "" : "=" + value;
                    replacements.add("-A" + key + suffix + " as -XepOpt:eisopcf:" + key + suffix);
                    break;
                }
            }
        }
        if (replacements.length() > 0) {
            throw new IllegalArgumentException(
                    "Under the Error Prone plugin, Checker Framework options are passed as Error"
                            + " Prone flags, not as javac -A options, which are ignored. Pass "
                            + replacements
                            + ".");
        }
    }

    /**
     * Checks that every host option is supported by at least one of the checkers, which is what
     * javac checks for {@code -A} options (it warns that an option "was not recognized by any
     * processor"). The host's options reach only these checkers, so an unrecognized one can only be
     * a mistake, such as a misspelling, and is reported as an error.
     *
     * <p>Called after {@link SourceChecker#init}, as javac calls {@link
     * SourceChecker#getSupportedOptions} after {@code init}: computing the supported options
     * instantiates the subcheckers, which needs the processing environment.
     *
     * @param options the host's options
     * @param checkers the initialized checkers
     * @throws IllegalArgumentException if an option is supported by none of the checkers
     */
    private static void checkOptionsAreSupported(
            Map<String, String> options, List<SourceChecker> checkers) {
        Set<String> unsupported = new LinkedHashSet<>(options.keySet());
        for (SourceChecker checker : checkers) {
            unsupported.removeAll(checker.getSupportedOptions());
        }
        if (!unsupported.isEmpty()) {
            throw new IllegalArgumentException(
                    "The following Checker Framework options are not recognized by any selected"
                            + " checker: "
                            + unsupported
                            + ". Each must be an option of a selected checker, written as it"
                            + " would be after -A (e.g. stubs or NullnessChecker_stubs).");
        }
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
        @Nullable ClassLoader[] candidates = {
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
                // className comes from the user-supplied eisopcf:checkers option, so it is not a
                // statically-known @BinaryName; an invalid name simply throws
                // ClassNotFoundException, handled below.
                @SuppressWarnings("signature:argument.type.incompatible")
                Class<?> loaded = Class.forName(className, true, loader);
                return loaded;
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
     * Runs all selected checkers over one package declaration.
     *
     * @param element the package being processed
     * @param path the tree path to the package declaration (leaf is a {@code PackageTree}), as the
     *     Checker Framework expects
     */
    public void processPackage(PackageElement element, TreePath path) {
        for (SourceChecker checker : checkers) {
            checker.packageProcessExternally(element, path);
        }
    }

    /**
     * Signals that all classes have been processed, running each checker's end-of-compilation step
     * (e.g. unneeded-suppression warnings) via {@code typeProcessingOverExternally}. Idempotent:
     * safe to call more than once (see {@link #finished}), so the end-of-compilation {@code
     * TaskListener} that invokes it need not track whether it has already fired.
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
