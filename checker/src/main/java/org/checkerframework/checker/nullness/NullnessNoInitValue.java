package org.checkerframework.checker.nullness;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;

/**
 * Behaves just like {@link CFValue}, but additionally tracks whether at this point {@link PolyNull}
 * is known to be {@link NonNull} or {@link Nullable} (or not known to be either)
 */
public class NullnessNoInitValue extends CFAbstractValue<NullnessNoInitValue> {

    /** True if, at this point, {@link PolyNull} is known to be {@link NonNull}. */
    protected boolean isPolyNullNonNull;

    /** True if, at this point, {@link PolyNull} is known to be {@link Nullable}. */
    protected boolean isPolyNullNull;

    /**
     * Creates a new NullnessValue.
     *
     * @param analysis the analysis
     * @param annotations the annotations
     * @param underlyingType the underlying type
     */
    public NullnessNoInitValue(
            CFAbstractAnalysis<NullnessNoInitValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }

    @Override
    protected NullnessNoInitValue upperBound(
            @Nullable NullnessNoInitValue other,
            TypeMirror upperBoundTypeMirror,
            boolean shouldWiden) {
        NullnessNoInitValue result = super.upperBound(other, upperBoundTypeMirror, shouldWiden);

        AnnotationMirror resultNullableAnno =
                analysis.getTypeFactory().getAnnotationByClass(result.annotations, Nullable.class);

        if (resultNullableAnno != null && other != null) {
            if ((this.isPolyNullNonNull
                            && this.containsNonNullOrPolyNull()
                            && other.isPolyNullNull
                            && other.containsNullableOrPolyNull())
                    || (other.isPolyNullNonNull
                            && other.containsNonNullOrPolyNull()
                            && this.isPolyNullNull
                            && this.containsNullableOrPolyNull())) {
                result.annotations.remove(resultNullableAnno);
                result.annotations.add(
                        ((NullnessNoInitAnnotatedTypeFactory) analysis.getTypeFactory()).POLYNULL);
            }
        }
        return result;
    }

    /**
     * Returns true if this value contains {@code @NonNull} or {@code @PolyNull}.
     *
     * @return true if this value contains {@code @NonNull} or {@code @PolyNull}
     */
    @Pure
    private boolean containsNonNullOrPolyNull() {
        return analysis.getTypeFactory().containsSameByClass(annotations, NonNull.class)
                || analysis.getTypeFactory().containsSameByClass(annotations, PolyNull.class);
    }

    /**
     * Returns true if this value contans {@code @Nullable} or {@code @PolyNull}.
     *
     * @return true if this value contans {@code @Nullable} or {@code @PolyNull}
     */
    @Pure
    private boolean containsNullableOrPolyNull() {
        return analysis.getTypeFactory().containsSameByClass(annotations, Nullable.class)
                || analysis.getTypeFactory().containsSameByClass(annotations, PolyNull.class);
    }

    @SideEffectFree
    @Override
    public String toStringSimple() {
        return "NV{"
                + AnnotationUtils.toStringSimple(annotations)
                + ", "
                + TypesUtils.simpleTypeName(underlyingType)
                + ", poly nn/n="
                + (isPolyNullNonNull ? 't' : 'f')
                + '/'
                + (isPolyNullNull ? 't' : 'f')
                + '}';
    }

    /**
     * Checks if this value is equal to another object.
     *
     * @param obj the object to compare to
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        NullnessNoInitValue other = (NullnessNoInitValue) obj;
        return this.isPolyNullNonNull == other.isPolyNullNonNull
                && this.isPolyNullNull == other.isPolyNullNull;
    }

    /** The cached hash code. */
    private int hashCodeCache = 0;

    /**
     * Computes the hash code for this value.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        if (hashCodeCache == 0) {
            int h = super.hashCode();
            h = 31 * h + (isPolyNullNonNull ? 1 : 0);
            h = 31 * h + (isPolyNullNull ? 1 : 0);
            hashCodeCache = h == 0 ? 1 : h;
        }
        return hashCodeCache;
    }
}
