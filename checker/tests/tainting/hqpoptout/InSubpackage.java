package hqpoptout.sub;

import org.checkerframework.checker.tainting.qual.PolyTainted;

// Package hqpoptout sets applyToSubpackages=false, so this class has no qualifier parameter and
// the polymorphic qualifier is rejected.
public class InSubpackage {
    // :: error: (invalid.polymorphic.qualifier.use)
    @PolyTainted int field;
}
