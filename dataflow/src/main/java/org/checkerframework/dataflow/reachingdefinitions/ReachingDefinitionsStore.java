package org.checkerframework.dataflow.reachingdefinitions;

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

/** A reaching definitions store contains a set of reaching definitions represented by nodes. */
public class ReachingDefinitionsStore implements Store<ReachingDefinitionsStore> {

    /** A set of reaching definitions abstract values. */
    private final Set<ReachingDefinitionsValue> reachingDefSet;

    /** Create a new ReachDefinitionStore. */
    public ReachingDefinitionsStore() {
        reachingDefSet = new LinkedHashSet<>();
    }

    /**
     * Create a new ReachDefinitionStore.
     *
     * @param reachingDefSet a set of reaching definitions abstract values
     */
    public ReachingDefinitionsStore(Set<ReachingDefinitionsValue> reachingDefSet) {
        this.reachingDefSet = reachingDefSet;
    }

    /**
     * Remove the information of a reaching definition from the reaching definitions set.
     *
     * @param defTarget target of a reaching definition
     */
    public void killDef(Node defTarget) {
        Iterator<ReachingDefinitionsValue> it = reachingDefSet.iterator();
        while (it.hasNext()) {
            ReachingDefinitionsValue existedDef = it.next();
            if (existedDef.def.getTarget().equals(defTarget)) {
                it.remove();
            }
        }
    }

    /**
     * Add the information of a reaching definition into the reaching definitions set.
     *
     * @param def a reaching definition
     */
    public void putDef(ReachingDefinitionsValue def) {
        reachingDefSet.add(def);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof ReachingDefinitionsStore)) {
            return false;
        }
        ReachingDefinitionsStore other = (ReachingDefinitionsStore) obj;
        return other.reachingDefSet.equals(this.reachingDefSet);
    }

    @Override
    public int hashCode() {
        return this.reachingDefSet.hashCode();
    }

    @Override
    public ReachingDefinitionsStore copy() {
        return new ReachingDefinitionsStore(new HashSet<>(reachingDefSet));
    }

    @Override
    public ReachingDefinitionsStore leastUpperBound(ReachingDefinitionsStore other) {
        Set<ReachingDefinitionsValue> reachingDefSetLub =
                new HashSet<>(this.reachingDefSet.size() + other.reachingDefSet.size());
        reachingDefSetLub.addAll(this.reachingDefSet);
        reachingDefSetLub.addAll(other.reachingDefSet);
        return new ReachingDefinitionsStore(reachingDefSetLub);
    }

    /** It should not be called since it is not used by the backward analysis. */
    @Override
    public ReachingDefinitionsStore widenedUpperBound(ReachingDefinitionsStore previous) {
        throw new BugInCF("wub of reaching definitions get called!");
    }

    @Override
    public boolean canAlias(JavaExpression a, JavaExpression b) {
        return true;
    }

    @Override
    public String visualize(CFGVisualizer<?, ReachingDefinitionsStore, ?> viz) {
        String key = "reaching definitions";
        if (reachingDefSet.isEmpty()) {
            return viz.visualizeStoreKeyVal(key, "none");
        }
        StringJoiner sjStoreVal = new StringJoiner(", ");
        for (ReachingDefinitionsValue reachDefValue : reachingDefSet) {
            sjStoreVal.add(reachDefValue.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return reachingDefSet.toString();
    }
}
