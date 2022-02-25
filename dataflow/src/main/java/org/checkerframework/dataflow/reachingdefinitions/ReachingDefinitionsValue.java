package org.checkerframework.dataflow.reachingdefinitions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.javacutil.BugInCF;

/** A reach definition (which is represented by a node) wrapper turning node into abstract value. */
public class ReachingDefinitionsValue implements AbstractValue<ReachingDefinitionsValue> {

    /**
     * A reach definition is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.AssignmentNode}.
     */
    protected final AssignmentNode def;

    @Override
    public ReachingDefinitionsValue leastUpperBound(ReachingDefinitionsValue other) {
        throw new BugInCF("lub of reachDef get called!");
    }

    /**
     * Create a new definition.
     *
     * @param n a node
     */
    public ReachingDefinitionsValue(AssignmentNode n) {
        this.def = n;
    }

    @Override
    public int hashCode() {
        return this.def.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachingDefinitionsValue)) {
            return false;
        }
        ReachingDefinitionsValue other = (ReachingDefinitionsValue) obj;
        return this.def.equals(other.def);
    }

    @Override
    public String toString() {
        return this.def.toString();
    }
}
