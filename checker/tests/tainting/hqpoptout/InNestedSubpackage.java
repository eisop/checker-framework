package hqpoptout.sub.nested;

import org.checkerframework.checker.tainting.qual.PolyTainted;

// The enclosing package opts out of all subpackages, including transitively nested ones.
public class InNestedSubpackage {
    // :: error: (invalid.polymorphic.qualifier.use)
    @PolyTainted int field;
}
