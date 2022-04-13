package org.checkerframework.dataflow.reachingdefinitions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizer;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.javacutil.BugInCF;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringJoiner;

/** A reaching definitions store records information about ReachingDefinitionsNode */
public class ReachingDefinitionsStore implements Store<ReachingDefinitionsStore> {

    /** The set of reaching definitions in this store */
    private final Set<ReachingDefinitionsNode> reachingDefSet;

    /** Create a new ReachDefinitionStore. */
    public ReachingDefinitionsStore() {
        reachingDefSet = new LinkedHashSet<>();
    }

    /**
     * Create a new ReachDefinitionStore.
     *
     * @param reachingDefSet a set of reaching definitions nodes, this parameter is captured and
     *     that the caller should not retain an alias
     */
    public ReachingDefinitionsStore(LinkedHashSet<ReachingDefinitionsNode> reachingDefSet) {
        this.reachingDefSet = reachingDefSet;
    }

    /**
     * Remove the information of a reaching definition from the reaching definitions set.
     *
     * @param defTarget target of a reaching definition
     */
    public void killDef(Node defTarget) {
        Iterator<ReachingDefinitionsNode> it = reachingDefSet.iterator();
        while (it.hasNext()) {
            ReachingDefinitionsNode generatedDefNode = it.next();
            // It's preferred to use "==" to compare two nodes in checker framework,
            // but in this case we use `equals` to only measure value equality.
            // If we use "==", two expressions from different nodes with same
            // abstract nodes will not consider as the same and cause the analysis
            // incorrect. Hence we use `equals` in place of `==`.
            if (generatedDefNode.def.getTarget().equals(defTarget)) {
                it.remove();
            }
        }
    }

    /**
     * Add the information of a reaching definition into the reaching definitions set.
     *
     * @param def a reaching definition
     */
    public void putDef(ReachingDefinitionsNode def) {
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
        return new ReachingDefinitionsStore(new LinkedHashSet<>(reachingDefSet));
    }

    @Override
    public ReachingDefinitionsStore leastUpperBound(ReachingDefinitionsStore other) {
        LinkedHashSet<ReachingDefinitionsNode> reachingDefSetLub =
                new LinkedHashSet<>(this.reachingDefSet.size() + other.reachingDefSet.size());
        reachingDefSetLub.addAll(this.reachingDefSet);
        reachingDefSetLub.addAll(other.reachingDefSet);
        return new ReachingDefinitionsStore(reachingDefSetLub);
    }

    /** We do not call widenedUpperBound in this analysis. */
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
        for (ReachingDefinitionsNode reachDefNode : reachingDefSet) {
            sjStoreVal.add(reachDefNode.toString());
        }
        return viz.visualizeStoreKeyVal(key, sjStoreVal.toString());
    }

    @Override
    public String toString() {
        return "ReachingDefinitionsStore: " + reachingDefSet.toString();
    }
}
