package org.checkerframework.checker.mutability;

import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

import java.util.Map;

/** The store for the mutability type system. */
public class MutabilityNoInitStore
        extends CFAbstractStore<MutabilityNoInitValue, MutabilityNoInitStore> {
    /** The initialized fields. */
    protected Map<@Immutable FieldAccess, MutabilityNoInitValue> initializedFields;

    /**
     * Create a new MutabilityNoInitStore.
     *
     * @param analysis the analysis
     * @param sequentialSemantics whether the analysis uses sequential semantics
     */
    public MutabilityNoInitStore(
            CFAbstractAnalysis<MutabilityNoInitValue, MutabilityNoInitStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /**
     * Create a new MutabilityNoInitStore.
     *
     * @param s the store to copy
     */
    public MutabilityNoInitStore(MutabilityNoInitStore s) {
        super(s);
        if (s.initializedFields != null) {
            initializedFields = s.initializedFields;
        }
    }
}
