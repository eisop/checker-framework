package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;

import org.checkerframework.checker.nullness.qual.Nullable;

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
 * <p>Only findings positioned at a {@link Tree} reach the sink. A finding positioned at an {@link
 * javax.lang.model.element.Element} (rather than a tree), or with no source position at all, is
 * still reported directly through javac's {@code Messager}, because such a finding has no {@code
 * Tree}/{@code TreePath} for a host to anchor its own diagnostic to. In Error Prone mode those
 * (rare) findings therefore appear as plain javac diagnostics rather than host diagnostics. Almost
 * all Checker Framework findings are positioned at a tree.
 *
 * <p>This interface intentionally uses only {@code javax.tools}, {@code com.sun.source.tree}, and
 * Checker Framework types, so the Checker Framework core has no dependency on any host framework
 * (in particular, no dependency on Error Prone). The translation from these neutral values to a
 * host-specific representation is the host's responsibility.
 */
@FunctionalInterface
public interface DiagnosticSink {

    /**
     * Receives one Checker Framework finding, which may carry machine-applicable suggested fixes
     * (as alternatives). A host with no fix pipeline simply ignores {@code fixes}, so this
     * interface can be implemented with a lambda.
     *
     * @param kind the diagnostic kind (typically {@link Diagnostic.Kind#ERROR} or {@link
     *     Diagnostic.Kind#WARNING})
     * @param message the fully-formatted, localized message text
     * @param source the tree at which the finding is reported; its source position locates the
     *     diagnostic. Never {@code null}: a finding with no tree position is reported through javac
     *     before reaching a sink (see above), so an installed sink is called only for
     *     tree-positioned findings.
     * @param root the compilation unit containing {@code source}
     * @param path the path to {@code source}, or null if it could not be determined. The checker
     *     computes it while visiting the finding, which is far cheaper than a host re-deriving it
     *     afterwards: by the time findings are handed over, locating a tree costs a scan of the
     *     whole compilation unit, so re-deriving it per finding is quadratic in the number of
     *     findings in a file.
     * @param fixes suggested fixes for the finding, as alternatives (possibly empty)
     */
    void report(
            Diagnostic.Kind kind,
            String message,
            Tree source,
            CompilationUnitTree root,
            @Nullable TreePath path,
            List<SuggestedFixData> fixes);
}
