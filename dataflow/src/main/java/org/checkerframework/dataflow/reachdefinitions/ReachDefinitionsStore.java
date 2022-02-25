package org.checkerframework.dataflow.reachdefinitions;

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
public class ReachDefinitionsStore implements Store<ReachDefinitionsStore> {

    /** A set of reach definitions abstract values. */
    private final Set<ReachDefinitionsValue> reachDefSet;

    /** Create a new ReachDefinitionStore. */
    public ReachDefinitionsStore() {
        reachDefSet = new LinkedHashSet<>();
    }

    /**
     * Create a new ReachDefinitionStore.
     *
     * @param reachDefSet a set of reach definition abstract values
     */
    public ReachDefinitionsStore(Set<ReachDefinitionsValue> reachDefSet) {
        this.reachDefSet = reachDefSet;
    }

    /**
     * Remove the information of a reach definition from the reach definition set.
     *
     * @param defTarget target of a reach definition
     */
    public void killDef(Node defTarget) {
        Iterator<ReachDefinitionsValue> it = reachDefSet.iterator();
        while (it.hasNext()) {
            ReachDefinitionsValue existedDef = it.next();
            if (existedDef.def.getTarget().toString().equals(defTarget.toString())) {
                it.remove();
            }
        }
    }

    /**
     * Add the information of a reach definition into the reach definition set.
     *
     * @param def a reach definition
     */
    public void putDef(ReachDefinitionsValue def) {
        reachDefSet.add(def);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachDefinitionsStore)) {
            return false;
        }
        ReachDefinitionsStore other = (ReachDefinitionsStore) obj;
        return other.reachDefSet.equals(this.reachDefSet);
    }

    @Override
    public int hashCode() {
        return this.reachDefSet.hashCode();
    }

    @Override
    public ReachDefinitionsStore copy() {
        return new ReachDefinitionsStore(new HashSet<>(reachDefSet));
    }

    @Override
    public ReachDefinitionsStore leastUpperBound(ReachDefinitionsStore other) {
        Set<ReachDefinitionsValue> reachDefSetLub =
                new HashSet<>(this.reachDefSet.size() + other.reachDefSet.size());
        reachDefSetLub.addAll(this.reachDefSet);
        reachDefSetLub.addAll(other.reachDefSet);
        return new ReachDefinitionsStore(reachDefSetLub);
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public ReachDefinitionsStore widenedUpperBound(ReachDefinitionsStore previous) {
        throw new BugInCF("wub of reach definition get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, ReachDefinitionsStore, ?> viz) {
        String key = "reach definitions";
        if (reachDefSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (ReachDefinitionsValue reachDefValue : reachDefSet) {
            sjStoreVal.add(reachDefValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return reachDefSet.toString();
    }
}
