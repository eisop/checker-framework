package org.checkerframework.dataflow.cfg.block;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.plumelib.util.ArraySet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Base class of the {@link Block} implementation hierarchy. */
public abstract class BlockImpl implements Block {

    /** The type of this basic block. */
    protected final BlockType type;

    /** The set of predecessors. */
    protected final Set<BlockImpl> predecessors;

    /** The unique ID for the next-created object. */
    private static final AtomicLong nextUid = new AtomicLong(0);

    /** The unique ID of this object. */
    private final transient long uid = nextUid.getAndIncrement();

    @Override
    public long getUid(@UnknownInitialization BlockImpl this) {
        return uid;
    }

    /**
     * Create a new BlockImpl.
     *
     * @param type the type of this basic block
     */
    protected BlockImpl(BlockType type) {
        this.type = type;
        // Most blocks have few predecessors.
        this.predecessors = new ArraySet<>(2);
    }

    @Override
    public BlockType getType() {
        return type;
    }

    @Override
    @SuppressWarnings("unchecked") // the result is a copy, so no BlockImpl-only alias exists
    public Set<Block> getPredecessors() {
        // Return a copy, because clients modify the predecessors while iterating over the result.
        // ArraySet#clone copies the array; the copy constructor checks every element for
        // duplicates, which is quadratic.
        return (Set<Block>) (Set<? extends Block>) ((ArraySet<BlockImpl>) predecessors).clone();
    }

    public void addPredecessor(BlockImpl pred) {
        predecessors.add(pred);
    }

    public void removePredecessor(BlockImpl pred) {
        predecessors.remove(pred);
    }
}
