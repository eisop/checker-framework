package org.checkerframework.dataflow.reachdefinition;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.javacutil.BugInCF;

/** A reach definition (which is represented by a node) wrapper turning node into abstract value. */
public class ReachDefinitionValue implements AbstractValue<ReachDefinitionValue> {

    /**
     * A reach definition is represented by a node, which can be a {@link
     * org.checkerframework.dataflow.cfg.node.AssignmentNode}.
     */
    protected final AssignmentNode defs;

    @Override
    public ReachDefinitionValue leastUpperBound(ReachDefinitionValue other) {
        throw new BugInCF("lub of reachDef get called!");
    }

    /**
     * Create a new definition.
     *
     * @param n a node
     */
    public ReachDefinitionValue(AssignmentNode n) {
        this.defs = n;
    }

    @Override
    public int hashCode() {
        return this.defs.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachDefinitionValue)) {
            return false;
        }
        ReachDefinitionValue other = (ReachDefinitionValue) obj;
        return this.defs.equals(other.defs);
    }

    @Override
    public String toString() {
        return this.defs.toString();
    }
}
