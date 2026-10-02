package hqp.sub.deep;

import org.checkerframework.checker.tainting.qual.PolyTainted;

/**
 * Package hqp.sub sets applyToSubpackages=false, which limits its own annotation to hqp.sub. It
 * does not block package hqp, whose annotation applies to subpackages and so still reaches here.
 * The class therefore has a qualifier parameter and the polymorphic qualifier is allowed; if the
 * walk up the package chain stopped at hqp.sub, this would be invalid.polymorphic.qualifier.use.
 */
public class Deep {
    @PolyTainted int field;
}
