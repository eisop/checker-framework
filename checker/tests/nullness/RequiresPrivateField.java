import org.checkerframework.checker.nullness.qual.*;

// @skip-test Test case for future feature:  @RequiresNonNull is permitted to access a private field
// (maybe just those that are annotated with @SpecPublic)

public class RequiresPrivateField {

    @RequiresNonNull("PptCombined.assemblies")
    public void testFindIntermediateBlocks1() {
        // no body
    }
}

class PptCombined {

    // :: warning: (monotonic.on.static)
    @SpecPublic private static @MonotonicNonNull String assemblies = null;
}
