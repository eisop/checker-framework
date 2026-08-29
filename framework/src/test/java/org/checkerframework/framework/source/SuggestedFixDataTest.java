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

    @Test(expected = UnsupportedOperationException.class)
    public void replacementsAreUnmodifiable() {
        SuggestedFixData fix = SuggestedFixData.replace(0, 1, "x");
        fix.getReplacements().add(new SuggestedFixData.Replacement(2, 3, "y"));
    }
}
