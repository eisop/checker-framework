package hqpoptin.sub.nested;

import org.checkerframework.checker.tainting.qual.PolyTainted;

// applyToSubpackages defaults to true, so the qualifier parameter reaches transitively nested
// subpackages.
public class InNestedSubpackage {
    @PolyTainted int field;
}
