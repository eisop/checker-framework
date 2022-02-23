package org.checkerframework.dataflow.reachdefinition;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

/** A reach definition store contains a set of reach definitions represented by nodes. */
public class ReachDefinitionStore implements Store<ReachDefinitionStore> {

    /** A set of reach definitions abstract values. */
    private final Set<ReachDefinitionValue> reachDefSet;

    /** Create a new ReachDefinitionStore. */
    public ReachDefinitionStore() {
        reachDefSet = new LinkedHashSet<>();
    }

    /**
     * Create a new ReachDefinitionStore.
     *
     * @param reachDefSet a set of reach definition abstract values
     */
    public ReachDefinitionStore(Set<ReachDefinitionValue> reachDefSet) {
        this.reachDefSet = reachDefSet;
    }

    /**
     * Remove the information of a reach definition from the reach definition set.
     *
     * @param defTarget target of a reach definition
     */
    public void killDef(Node defTarget) {
        Iterator<ReachDefinitionValue> it = reachDefSet.iterator();
        while (it.hasNext()) {
            ReachDefinitionValue existedDef = it.next();
            if (existedDef.defs.getTarget().toString().equals(defTarget.toString())) {
                it.remove();
            }
        }
    }

    /**
     * Add the information of a reach definition into the reach definition set.
     *
     * @param def a reach definition
     */
    public void putDef(ReachDefinitionValue def) {
        reachDefSet.add(def);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachDefinitionStore)) {
            return false;
        }
        ReachDefinitionStore other = (ReachDefinitionStore) obj;
        return other.reachDefSet.equals(this.reachDefSet);
    }

    @Override
    public int hashCode() {
        return this.reachDefSet.hashCode();
    }

    @Override
    public ReachDefinitionStore copy() {
        return new ReachDefinitionStore(new HashSet<>(reachDefSet));
    }

    @Override
    public ReachDefinitionStore leastUpperBound(ReachDefinitionStore other) {
        Set<ReachDefinitionValue> reachDefSetLub =
                new HashSet<>(this.reachDefSet.size() + other.reachDefSet.size());
        reachDefSetLub.addAll(this.reachDefSet);
        reachDefSetLub.addAll(other.reachDefSet);
        return new ReachDefinitionStore(reachDefSetLub);
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public ReachDefinitionStore widenedUpperBound(ReachDefinitionStore previous) {
        throw new BugInCF("wub of reach definition get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, ReachDefinitionStore, ?> viz) {
        String key = "reach definitions";
        if (reachDefSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (ReachDefinitionValue reachDefValue : reachDefSet) {
            sjStoreVal.add(reachDefValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return reachDefSet.toString();
    }
}
