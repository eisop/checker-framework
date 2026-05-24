package org.checkerframework.framework.util.defaults;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.javacutil.AnnotationUtils;

import java.util.Objects;

import javax.lang.model.element.AnnotationMirror;

/**
 * Represents a mapping from an Annotation to a TypeUseLocation it should be applied to during
 * defaulting. The Comparable ordering of this class first tests location then tests annotation
 * ordering (via {@link org.checkerframework.javacutil.AnnotationUtils}).
 *
 * <p>It also has a handy toString method that is useful for debugging.
 */
public class Default implements Comparable<Default> {
    // please remember to add any fields to the hashcode calculation
    /** The default annotation mirror. */
    public final AnnotationMirror anno;

    /** The type use location. */
    public final TypeUseLocation location;

    /** Whether the default should be inherited by subpackages. */
    public final boolean applyToSubpackages;

    /**
     * Construct a Default object.
     *
     * @param anno the default annotation mirror
     * @param location the type use location
     * @param applyToSubpackages whether the default should be inherited by subpackages
     */
    public Default(AnnotationMirror anno, TypeUseLocation location, boolean applyToSubpackages) {
        this.anno = anno;
        this.location = location;
        this.applyToSubpackages = applyToSubpackages;
    }

    @Override
    public int compareTo(Default other) {
        int locationOrder = location.compareTo(other.location);
        if (locationOrder != 0) {
            return locationOrder;
        }
        int annoOrder = AnnotationUtils.compareAnnotationMirrors(anno, other.anno);
        if (annoOrder != 0) {
            return annoOrder;
        }
        return Boolean.compare(applyToSubpackages, other.applyToSubpackages);
    }

    @Override
    public boolean equals(@Nullable Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (thatObj == null || thatObj.getClass() != Default.class) {
            return false;
        }

        return compareTo((Default) thatObj) == 0;
    }

    @Override
    public int hashCode() {
        // equals() delegates to compareTo(), which uses AnnotationUtils.compareAnnotationMirrors
        // for the AnnotationMirror field. Object.hashCode() on an AnnotationMirror is not
        // guaranteed to be consistent with compareAnnotationMirrors (the same annotation can be
        // represented by different implementing classes with different hashCode()s), so hash via
        // AnnotationUtils.hashCode, which is documented to be consistent with areSame.
        return Objects.hash(AnnotationUtils.hashCode(anno), location, applyToSubpackages);
    }

    @Override
    public String toString() {
        return "( "
                + location.name()
                + " => "
                + anno
                + (applyToSubpackages ? " applies to subpackages" : "")
                + " )";
    }
}
