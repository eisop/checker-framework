package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * A machine-applicable source edit that a {@link SourceChecker} may attach to a finding, so a host
 * (such as the Error Prone plugin) can offer it as a suggested fix through its own fix / patch
 * pipeline.
 *
 * <p>A fix is a list of {@link Replacement text replacements}, each expressed with javac
 * source-offset positions (the same character offsets used by {@code
 * com.sun.source.util.SourcePositions} and {@code com.sun.source.tree.EndPosTable}). These are
 * JDK-neutral values: this class references no host-framework (in particular no Error Prone) type,
 * so the Checker Framework core stays framework-agnostic. Translating a {@code SuggestedFixData}
 * into a host-specific fix (e.g. an Error Prone {@code SuggestedFix}) is the host's responsibility.
 *
 * <p>Most Checker Framework findings do not carry a fix. A checker that can compute one attaches it
 * with {@link DiagMessage#withFixes}; see {@code NullnessNoInitVisitor} for an example.
 */
public final class SuggestedFixData {

    /** A single text replacement: replace the source between two offsets with new text. */
    public static final class Replacement {
        /** Start source offset, inclusive. */
        public final int startPosition;

        /** End source offset, exclusive. */
        public final int endPosition;

        /** The replacement text (empty string to delete the range). */
        public final String text;

        /**
         * Creates a replacement.
         *
         * @param startPosition start source offset, inclusive
         * @param endPosition end source offset, exclusive
         * @param text the replacement text (empty to delete)
         */
        public Replacement(int startPosition, int endPosition, String text) {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
            this.text = text;
        }
    }

    /** The replacements that make up this fix, applied together. Unmodifiable. */
    private final List<Replacement> replacements;

    /**
     * Creates a fix from the given replacements.
     *
     * @param replacements the replacements that make up this fix
     */
    public SuggestedFixData(List<Replacement> replacements) {
        this.replacements = Collections.unmodifiableList(new ArrayList<>(replacements));
    }

    /**
     * Returns the replacements that make up this fix. The returned list is unmodifiable.
     *
     * @return the replacements, applied together
     */
    public List<Replacement> getReplacements() {
        return replacements;
    }

    /**
     * Convenience factory for a single-replacement fix.
     *
     * @param startPosition start source offset, inclusive
     * @param endPosition end source offset, exclusive
     * @param text the replacement text
     * @return a fix consisting of the one replacement
     */
    public static SuggestedFixData replace(int startPosition, int endPosition, String text) {
        return new SuggestedFixData(
                Collections.singletonList(new Replacement(startPosition, endPosition, text)));
    }

    /**
     * Returns a fix that replaces the source range of {@code tree} with {@code text}, using javac's
     * {@link SourcePositions} to locate the tree. All arguments are JDK types, so a checker can
     * build a fix without referencing any host framework.
     *
     * @param sourcePositions javac source positions (e.g. from {@code trees.getSourcePositions()})
     * @param root the compilation unit containing {@code tree}
     * @param tree the tree whose source range to replace
     * @param text the replacement text (empty to delete)
     * @return the fix, or {@code null} if {@code tree}'s source position is unavailable
     */
    @SuppressWarnings("removal") // SourcePositions#getStartPosition/getEndPosition
    public static @Nullable SuggestedFixData replaceTree(
            SourcePositions sourcePositions, CompilationUnitTree root, Tree tree, String text) {
        long start = sourcePositions.getStartPosition(root, tree);
        long end = sourcePositions.getEndPosition(root, tree);
        if (start == Diagnostic.NOPOS || end == Diagnostic.NOPOS) {
            return null;
        }
        return replace((int) start, (int) end, text);
    }

    /**
     * Returns a fix that deletes {@code tree} along with any whitespace immediately following it.
     * Deleting the trailing whitespace avoids leaving a stray space behind: for example, deleting
     * the {@code @Nullable} annotation from {@code @Nullable int x} leaves {@code int x} rather
     * than a leading space before {@code int}. Uses javac's {@link SourcePositions} and the
     * compilation unit's source text; all arguments are JDK types. If the source text is
     * unavailable, only {@code tree}'s own source range is deleted.
     *
     * @param sourcePositions javac source positions (e.g. from {@code trees.getSourcePositions()})
     * @param root the compilation unit containing {@code tree}
     * @param tree the tree to delete
     * @return the fix, or {@code null} if {@code tree}'s source position is unavailable
     */
    @SuppressWarnings("removal") // SourcePositions#getStartPosition/getEndPosition
    public static @Nullable SuggestedFixData deleteTree(
            SourcePositions sourcePositions, CompilationUnitTree root, Tree tree) {
        long start = sourcePositions.getStartPosition(root, tree);
        long end = sourcePositions.getEndPosition(root, tree);
        if (start == Diagnostic.NOPOS || end == Diagnostic.NOPOS) {
            return null;
        }
        // getSourceFile() and getCharContent() may each return null, and getCharContent() may
        // throw if the file cannot be read.  In any of those cases, fall back to deleting only the
        // tree's own source range.
        JavaFileObject sourceFile = root.getSourceFile();
        CharSequence source;
        if (sourceFile == null) {
            source = null;
        } else {
            try {
                source = sourceFile.getCharContent(true);
            } catch (IOException e) {
                source = null;
            }
        }
        int deleteEnd = (int) end;
        if (source != null) {
            while (deleteEnd < source.length()
                    && Character.isWhitespace(source.charAt(deleteEnd))) {
                deleteEnd++;
            }
        }
        return replace((int) start, deleteEnd, "");
    }
}
