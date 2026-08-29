package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import java.util.List;

import javax.tools.Diagnostic;

/**
 * A destination for Checker Framework diagnostics, allowing a host to intercept findings instead of
 * having them printed through javac's {@link com.sun.source.util.Trees#printMessage Trees}.
 *
 * <p>By default a {@link SourceChecker} reports each finding directly to javac. A host that embeds
 * the Checker Framework can install a {@code DiagnosticSink} (see {@link
 * SourceChecker#setDiagnosticSink}) to receive findings and report them through its own channel
 * instead. This is used when the Checker Framework runs as an Error Prone plugin, so that Checker
 * Framework findings become Error Prone {@code Description}s (honoring Error Prone severity,
 * suppression, and the suggested-fix / patch pipeline).
 *
 * <p>This interface intentionally uses only {@code javax.tools}, {@code com.sun.source.tree}, and
 * Checker Framework types, so the Checker Framework core has no dependency on any host framework
 * (in particular, no dependency on Error Prone). The translation from these neutral values to a
 * host-specific representation is the host's responsibility.
 */
@FunctionalInterface
public interface DiagnosticSink {

    /**
     * Receives one Checker Framework finding.
     *
     * @param kind the diagnostic kind (typically {@link Diagnostic.Kind#ERROR} or {@link
     *     Diagnostic.Kind#WARNING})
     * @param message the fully-formatted, localized message text
     * @param source the tree at which the finding is reported (its source position locates the
     *     diagnostic); may be {@code null} if no tree is associated
     * @param root the compilation unit containing {@code source}
     */
    void report(Diagnostic.Kind kind, String message, Tree source, CompilationUnitTree root);

    /**
     * Receives one Checker Framework finding that may carry machine-applicable suggested fixes (as
     * alternatives).
     *
     * <p>The default implementation ignores the fixes and delegates to {@link #report}, so existing
     * single-method (lambda) sinks keep working. A host that supports fixes (such as the Error
     * Prone plugin) overrides this to translate each fix into its own representation and attach it
     * to the reported finding.
     *
     * <p>The Checker Framework does not yet produce fixes for its findings in general; this method
     * defines the neutral channel so that, once a checker does, fixes reach a host's patch pipeline
     * without any further core change.
     *
     * @param kind the diagnostic kind
     * @param message the fully-formatted, localized message text
     * @param source the tree at which the finding is reported; may be {@code null}
     * @param root the compilation unit containing {@code source}
     * @param fixes suggested fixes for the finding, as alternatives (possibly empty)
     */
    default void reportWithFix(
            Diagnostic.Kind kind,
            String message,
            Tree source,
            CompilationUnitTree root,
            List<SuggestedFixData> fixes) {
        report(kind, message, source, root);
    }
}
