package org.checkerframework.checker.pico;

import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

import java.util.Map;

/** The store for the immutability type system. */
public class PICONoInitStore extends CFAbstractStore<PICONoInitValue, PICONoInitStore> {
    /** The initialized fields. */
    protected Map<@Immutable FieldAccess, PICONoInitValue> initializedFields;

    /**
     * Create a new PICONoInitStore.
     *
     * @param analysis the analysis
     * @param sequentialSemantics whether the analysis uses sequential semantics
     */
    public PICONoInitStore(
            CFAbstractAnalysis<PICONoInitValue, PICONoInitStore, ?> analysis,
            boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    /**
     * Create a new PICONoInitStore.
     *
     * @param s the store to copy
     */
    public PICONoInitStore(PICONoInitStore s) {
        super(s);
        if (s.initializedFields != null) {
            initializedFields = s.initializedFields;
        }
    }
}
