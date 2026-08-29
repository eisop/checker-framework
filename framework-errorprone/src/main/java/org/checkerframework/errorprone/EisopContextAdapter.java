package org.checkerframework.errorprone;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import javax.annotation.processing.ProcessingEnvironment;

/**
 * Bridges Error Prone's javac {@link Context} to the {@link ProcessingEnvironment} that the Checker
 * Framework's {@code SourceChecker} expects.
 *
 * <p>When the Checker Framework runs standalone it is a javac annotation processor and receives a
 * {@link JavacProcessingEnvironment} directly. When it runs as an Error Prone plugin, an Error
 * Prone {@code BugChecker} instead has a {@code VisitorState} whose public {@code context} field is
 * the javac {@link Context}. The {@link JavacProcessingEnvironment} singleton remains registered in
 * that {@link Context} through the ANALYZE phase (when Error Prone runs), so it can be retrieved
 * and handed to {@code SourceChecker.init}. {@code SourceChecker.unwrapProcessingEnvironment}
 * recognizes a {@link JavacProcessingEnvironment} and uses it as-is, so this is exactly the
 * environment the CF uses in standalone mode.
 *
 * <p>This class contains no Error Prone types; it depends only on the JDK compiler API. That keeps
 * the Context-to-ProcessingEnvironment concern testable without constructing an Error Prone {@code
 * VisitorState}.
 */
public final class EisopContextAdapter {

    /** Do not instantiate. */
    private EisopContextAdapter() {
        throw new AssertionError("do not instantiate");
    }

    /**
     * Returns the {@link ProcessingEnvironment} registered in the given javac {@link Context}.
     *
     * @param context the javac context (e.g. Error Prone's {@code VisitorState.context})
     * @return the {@link JavacProcessingEnvironment} for that context, as a {@link
     *     ProcessingEnvironment}
     * @throws IllegalStateException if no {@link JavacProcessingEnvironment} is registered in the
     *     context (for example, if annotation processing was disabled with {@code -proc:none})
     */
    public static ProcessingEnvironment getProcessingEnvironment(Context context) {
        JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
        if (env == null) {
            throw new IllegalStateException(
                    "No JavacProcessingEnvironment is registered in the javac Context. The Checker"
                            + " Framework Error Prone plugin requires annotation processing to be"
                            + " enabled (do not pass -proc:none).");
        }
        return env;
    }

    /**
     * Returns the fully-qualified package name of the Checker Framework dataflow {@code
     * ControlFlowGraph} class visible on the current classloader.
     *
     * <p>Error Prone bundles a package-<em>relocated</em> copy of the dataflow library (shaded to
     * {@code org.checkerframework.errorprone.dataflow...}), whereas the Checker Framework core uses
     * the un-relocated {@code org.checkerframework.dataflow}. When a CF checker runs under Error
     * Prone it must use the un-relocated classes. This helper exposes which copy is actually loaded
     * so a test (and, if ever needed, a runtime guard) can verify the correct one is in effect.
     *
     * @return the package name of the loaded {@code ControlFlowGraph} class (expected to be {@code
     *     org.checkerframework.dataflow.cfg}), never the relocated {@code
     *     org.checkerframework.errorprone.dataflow.cfg}
     * @throws IllegalStateException if the Checker Framework dataflow library is not on the
     *     classpath
     */
    public static String loadedDataflowPackage() {
        try {
            Class<?> cfg = Class.forName("org.checkerframework.dataflow.cfg.ControlFlowGraph");
            Package pkg = cfg.getPackage();
            return pkg == null ? "" : pkg.getName();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "The Checker Framework dataflow library (org.checkerframework.dataflow) is not"
                            + " on the classpath.",
                    e);
        }
    }

    /**
     * Returns the {@link Trees} instance for the given context, obtained through the {@link
     * ProcessingEnvironment}. Provided as a convenience for callers that need tree utilities.
     *
     * @param context the javac context
     * @return the {@link Trees} instance for the context
     */
    public static Trees getTrees(Context context) {
        return Trees.instance(getProcessingEnvironment(context));
    }
}
