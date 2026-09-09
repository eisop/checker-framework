package hqpoptout;

import org.checkerframework.checker.tainting.qual.PolyTainted;

// The package's @HasQualifierParameter still covers the package itself, so a polymorphic
// qualifier may be written on this field.
public class InPackage {
    @PolyTainted int field;
}
