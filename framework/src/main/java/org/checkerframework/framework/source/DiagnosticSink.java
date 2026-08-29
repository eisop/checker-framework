package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

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
 * <p>This interface intentionally uses only {@code javax.tools} and {@code com.sun.source.tree}
 * types, so the Checker Framework core has no dependency on any host framework (in particular, no
 * dependency on Error Prone). The translation from these neutral values to a host-specific
 * representation is the host's responsibility.
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
}
