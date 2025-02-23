package org.checkerframework.checker.nullness;

import org.checkerframework.checker.initialization.InitializationAnnotatedTypeFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.qual.MonotonicQualifier;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * In addition to the base class behavior, tracks whether {@link PolyNull} is known to be {@link
 * NonNull} or {@link Nullable} (or not known to be either).
 */
public class NullnessNoInitStore extends CFAbstractStore<NullnessNoInitValue, NullnessNoInitStore> {

    /** True if, at this point, {@link PolyNull} is known to be {@link NonNull}. */
    protected boolean isPolyNullNonNull;

    /** True if, at this point, {@link PolyNull} is known to be {@link Nullable}. */
    protected boolean isPolyNullNull;

    /**
     * Initialized fields and their values.
     *
     * <p>This is used by {@link #newFieldValueAfterMethodCall(FieldAccess,
     * GenericAnnotatedTypeFactory, NullnessNoInitValue)} as cache to avoid performance issue in
     * #1438.
     *
     * @see
     *     InitializationAnnotatedTypeFactory#isInitialized(org.checkerframework.framework.type.GenericAnnotatedTypeFactory,
     *     org.checkerframework.framework.flow.CFAbstractValue,
     *     javax.lang.model.element.VariableElement)
     */
    protected Map<FieldAccess, NullnessNoInitValue> initializedFields;

    /**
     * Create a NullnessStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics (i.e., assume
     *     that only one thread is running at all times)?
     */
    public NullnessNoInitStore(
            CFAbstractAnalysis<NullnessNoInitValue, NullnessNoInitStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
        isPolyNullNonNull = false;
        isPolyNullNull = false;
    }

    /**
     * Create a NullnessStore (copy constructor).
     *
     * @param s a store to copy
     */
    public NullnessNoInitStore(NullnessNoInitStore s) {
        super(s);
        isPolyNullNonNull = s.isPolyNullNonNull;
        isPolyNullNull = s.isPolyNullNull;
        if (s.initializedFields != null) {
            initializedFields = s.initializedFields;
        }
    }

    @Override
    protected NullnessNoInitValue newFieldValueAfterMethodCall(
            FieldAccess fieldAccess,
            GenericAnnotatedTypeFactory<NullnessNoInitValue, NullnessNoInitStore, ?, ?>
                    atypeFactory,
            NullnessNoInitValue value) {
        // If the field is unassignable, it cannot change; thus we keep
        // its current value.
        // Unassignable fields must be handled before initialized fields
        // because in the case of a field that is both unassignable and
        // initialized, the initializedFields cache may contain an older,
        // less refined value.
        if (!fieldAccess.isAssignableByOtherCode()) {
            return value;
        }

        if (initializedFields == null) {
            initializedFields = new HashMap<>(4);
        }

        // If the field is initialized, it can change, but cannot be uninitialized.
        // We thus keep a new value based on its declared type.
        if (initializedFields.containsKey(fieldAccess)) {
            return initializedFields.get(fieldAccess);
        } else if (InitializationAnnotatedTypeFactory.isInitialized(
                        atypeFactory, value, fieldAccess.getField())
                && atypeFactory
                        .getAnnotationWithMetaAnnotation(
                                fieldAccess.getField(), MonotonicQualifier.class)
                        .isEmpty()) {

            NullnessNoInitValue newValue =
                    analysis.createAbstractValue(
                            atypeFactory.getAnnotatedType(fieldAccess.getField()).getAnnotations(),
                            value.getUnderlyingType());
            initializedFields.put(fieldAccess, newValue);
            return newValue;
        }

        // If the field has a monotonic annotation, we use the superclass's
        // handling of monotonic annotations.
        return super.newMonotonicFieldValueAfterMethodCall(fieldAccess, atypeFactory, value);
    }

    @Override
    public NullnessNoInitStore leastUpperBound(NullnessNoInitStore other) {
        NullnessNoInitStore lub = super.leastUpperBound(other);
        lub.isPolyNullNonNull = isPolyNullNonNull && other.isPolyNullNonNull;
        lub.isPolyNullNull = isPolyNullNull && other.isPolyNullNull;
        return lub;
    }

    @Override
    protected boolean supersetOf(CFAbstractStore<NullnessNoInitValue, NullnessNoInitStore> o) {
        if (!(o instanceof NullnessNoInitStore)) {
            return false;
        }
        NullnessNoInitStore other = (NullnessNoInitStore) o;
        if ((other.isPolyNullNonNull != isPolyNullNonNull)
                || (other.isPolyNullNull != isPolyNullNull)) {
            return false;
        }
        return super.supersetOf(other);
    }

    @Override
    protected String internalVisualize(
            CFGVisualizer<NullnessNoInitValue, NullnessNoInitStore, ?> viz) {
        return super.internalVisualize(viz)
                + viz.getSeparator()
                + viz.visualizeStoreKeyVal("isPolyNullNonNull", isPolyNullNonNull)
                + viz.getSeparator()
                + viz.visualizeStoreKeyVal("isPolyNullNull", isPolyNullNull);
    }

    /**
     * Returns true if, at this point, {@link PolyNull} is known to be {@link NonNull}.
     *
     * @return true if, at this point, {@link PolyNull} is known to be {@link NonNull}
     */
    public boolean isPolyNullNonNull() {
        return isPolyNullNonNull;
    }

    /**
     * Set the value of whether, at this point, {@link PolyNull} is known to be {@link NonNull}.
     *
     * @param isPolyNullNonNull whether, at this point, {@link PolyNull} is known to be {@link
     *     NonNull}
     */
    public void setPolyNullNonNull(boolean isPolyNullNonNull) {
        this.isPolyNullNonNull = isPolyNullNonNull;
    }

    /**
     * Returns true if, at this point, {@link PolyNull} is known to be {@link Nullable}.
     *
     * @return true if, at this point, {@link PolyNull} is known to be {@link Nullable}
     */
    public boolean isPolyNullNull() {
        return isPolyNullNull;
    }

    /**
     * Set the value of whether, at this point, {@link PolyNull} is known to be {@link Nullable}.
     *
     * @param isPolyNullNull whether, at this point, {@link PolyNull} is known to be {@link
     *     Nullable}
     */
    public void setPolyNullNull(boolean isPolyNullNull) {
        this.isPolyNullNull = isPolyNullNull;
    }
}
