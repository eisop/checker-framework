package org.checkerframework.framework.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 * <p>The Checker Framework does not yet produce these for its findings in general; this type
 * establishes the neutral representation so that fixes flow through to a host's patch pipeline as
 * soon as a checker does produce them.
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

    /** The replacements that make up this fix, applied together. */
    private final List<Replacement> replacements;

    /**
     * Creates a fix from the given replacements.
     *
     * @param replacements the replacements that make up this fix
     */
    public SuggestedFixData(List<Replacement> replacements) {
        this.replacements = new ArrayList<>(replacements);
    }

    /**
     * Returns the replacements that make up this fix.
     *
     * @return the replacements, applied together
     */
    public List<Replacement> getReplacements() {
        return Collections.unmodifiableList(replacements);
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
}
