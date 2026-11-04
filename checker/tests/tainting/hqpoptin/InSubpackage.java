package hqpoptin.sub;

import org.checkerframework.checker.tainting.qual.PolyTainted;

// applyToSubpackages defaults to true, so this class inherits the qualifier parameter from package
// hqpoptin and the polymorphic qualifier is allowed.
public class InSubpackage {
    @PolyTainted int field;
}
