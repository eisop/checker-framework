import org.checkerframework.framework.qual.DefaultQualifier;
import org.jspecify.annotations.NonNull;

/**
 * A redundant annotation written using an alias, rather than the canonical qualifier, must be
 * reported too: getExplicitAnnotations() returns it as written (possibly an alias), so the
 * redundant-annotation comparison must canonicalize it before comparing against the (always
 * canonical) default.
 *
 * <p>Uses the jspecify alias as the sole import (not the canonical
 * org.checkerframework.checker.nullness.qual.NonNull too): mixing an imported simple name with an
 * inline fully-qualified reference to a different class of the same simple name breaks javac's
 * parsing of a type-use annotation (see UnrecognizedLocationsInstanceOf.java for the same
 * workaround, for the same reason).
 */
@DefaultQualifier(org.checkerframework.checker.nullness.qual.NonNull.class)
public class RedundantAliasAnnotation {
    // :: warning: (redundant.anno)
    @NonNull String alias = "b";
}
