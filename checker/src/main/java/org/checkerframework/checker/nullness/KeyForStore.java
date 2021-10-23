package org.checkerframework.checker.nullness;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;

public class KeyForStore extends CFAbstractStore<KeyForValue, KeyForStore> {
    /**
     * Constructor to create a non-bottom KeyForStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics?
     */
    public KeyForStore(
            CFAbstractAnalysis<KeyForValue, KeyForStore, ?> analysis, boolean sequentialSemantics) {
        this(analysis, sequentialSemantics, false);
    }

    /**
     * Constructor for KeyForStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics?
     * @param isBottom is the store a bottom store?
     */
    public KeyForStore(
            CFAbstractAnalysis<KeyForValue, KeyForStore, ?> analysis,
            boolean sequentialSemantics,
            boolean isBottom) {
        super(analysis, sequentialSemantics, isBottom);
    }

    /**
     * Copy constructor.
     *
     * @param other the instance of KeyForStore to copy from
     */
    protected KeyForStore(CFAbstractStore<KeyForValue, KeyForStore> other) {
        super(other);
    }
}
