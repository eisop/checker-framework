package org.checkerframework.framework.flow;

/** The default store used in the Checker Framework. */
public class CFStore extends CFAbstractStore<CFValue, CFStore> {

    /**
     * Constructor to create a non-bottom CFStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics?
     */
    public CFStore(CFAbstractAnalysis<CFValue, CFStore, ?> analysis, boolean sequentialSemantics) {
        this(analysis, sequentialSemantics, false);
    }

    /**
     * Constructor for CFStore.
     *
     * @param analysis the analysis class this store belongs to
     * @param sequentialSemantics should the analysis use sequential Java semantics?
     * @param isBottom is the store a bottom store?
     */
    public CFStore(
            CFAbstractAnalysis<CFValue, CFStore, ?> analysis,
            boolean sequentialSemantics,
            boolean isBottom) {
        super(analysis, sequentialSemantics, isBottom);
    }

    /**
     * Copy constructor.
     *
     * @param other the CFStore to copy
     */
    public CFStore(CFAbstractStore<CFValue, CFStore> other) {
        super(other);
    }
}
