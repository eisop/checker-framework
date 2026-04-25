package org.checkerframework.framework.type;

import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;

/**
 * Computes the hashcode of an AnnotatedTypeMirror using the underlying type and primary annotations
 * and the hash code of component types of AnnotatedTypeMirror.
 *
 * <p>This class should be synchronized with EqualityAtmComparer.
 *
 * <p>The visitor accumulates into a mutable {@code int} field rather than returning {@code Integer}
 * through the scanner's reduce machinery. This avoids per-node {@code Integer.valueOf} boxing and
 * the lambda dispatch through {@code reduceFunction}, which together accounted for roughly 2.5% of
 * total CPU in profiling.
 *
 * @see org.checkerframework.framework.type.EqualityAtmComparer for more details.
 *     <p>This is used by AnnotatedTypeMirror.hashcode.
 */
public class HashcodeAtmVisitor extends SimpleAnnotatedTypeScanner<Void, Void> {

    /** Accumulator for the hash currently being computed. Reset by {@link #reset}. */
    private int hash;

    /** Creates a {@link HashcodeAtmVisitor}. */
    public HashcodeAtmVisitor() {
        // No reduce function or default result needed: this visitor accumulates into the
        // `hash` field rather than threading values through the scanner's reduce machinery.
        super();
    }

    /**
     * Computes and returns the hash of {@code type}.
     *
     * @param type the type to hash
     * @return the hash code of {@code type}
     */
    public int compute(AnnotatedTypeMirror type) {
        // visit() calls reset() then scan(); reset() zeros the accumulator.
        visit(type);
        return hash;
    }

    @Override
    public void reset() {
        super.reset();
        hash = 0;
    }

    /**
     * Hashes {@code type} using the underlying type and the primary annotation, accumulating into
     * {@link #hash}. This method does not descend into component types (this occurs in the scan
     * method).
     *
     * @param type the type
     * @param v unused
     * @return unused
     */
    @Override
    protected Void defaultAction(AnnotatedTypeMirror type, Void v) {
        // Null types are allowed, to differentiate between partially initialized types (which
        // may have null components) and fully initialized types.
        if (type == null) {
            return null;
        }
        int leaf = type.getUnderlyingTypeHashCode();
        leaf = 31 * leaf + type.getAnnotationsField().hashCode();
        hash = 31 * hash + leaf;
        return null;
    }
}
