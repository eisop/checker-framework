package org.checkerframework.framework.source;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for {@link SuggestedFixData}, the framework-agnostic (Error-Prone-free) representation
 * of a machine-applicable source edit that a host can translate into its own suggested fix.
 */
public class SuggestedFixDataTest {

    /** The single-replacement {@code replace} factory populates one replacement with its fields. */
    @Test
    public void singleReplacementFactory() {
        SuggestedFixData fix = SuggestedFixData.replace(3, 7, "@NonNull ");
        List<SuggestedFixData.Replacement> replacements = fix.getReplacements();
        assertEquals(1, replacements.size());
        SuggestedFixData.Replacement r = replacements.get(0);
        assertEquals(3, r.startPosition);
        assertEquals(7, r.endPosition);
        assertEquals("@NonNull ", r.text);
    }

    /** A fix built from several replacements preserves their order. */
    @Test
    public void multipleReplacementsPreserveOrder() {
        SuggestedFixData fix =
                new SuggestedFixData(
                        Arrays.asList(
                                new SuggestedFixData.Replacement(0, 0, "import a.B;\n"),
                                new SuggestedFixData.Replacement(10, 12, "B")));
        List<SuggestedFixData.Replacement> replacements = fix.getReplacements();
        assertEquals(2, replacements.size());
        assertEquals("import a.B;\n", replacements.get(0).text);
        assertEquals(10, replacements.get(1).startPosition);
    }

    /** The list returned by {@link SuggestedFixData#getReplacements} is unmodifiable. */
    @Test(expected = UnsupportedOperationException.class)
    public void replacementsAreUnmodifiable() {
        SuggestedFixData fix = SuggestedFixData.replace(0, 1, "x");
        fix.getReplacements().add(new SuggestedFixData.Replacement(2, 3, "y"));
    }

    /** A replacement with end before start is rejected at construction. */
    @Test(expected = IllegalArgumentException.class)
    public void replacementRejectsEndBeforeStart() {
        new SuggestedFixData.Replacement(5, 3, "x");
    }

    /** A replacement with a negative start offset is rejected at construction. */
    @Test(expected = IllegalArgumentException.class)
    public void replacementRejectsNegativeStart() {
        new SuggestedFixData.Replacement(-1, 0, "x");
    }

    /** A zero-width replacement (a pure insertion at one offset) is allowed. */
    @Test
    public void replacementAllowsEmptyRange() {
        SuggestedFixData.Replacement r = new SuggestedFixData.Replacement(4, 4, "inserted");
        assertEquals(4, r.startPosition);
        assertEquals(4, r.endPosition);
    }
}
