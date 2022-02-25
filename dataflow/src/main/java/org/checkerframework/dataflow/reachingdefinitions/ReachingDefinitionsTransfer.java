package org.checkerframework.dataflow.reachingdefinitions;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ForwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.AbstractNodeVisitor;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;

import java.util.List;

/** A reaching definitions transfer function. */
public class ReachingDefinitionsTransfer
        extends AbstractNodeVisitor<
                TransferResult<ReachingDefinitionsValue, ReachingDefinitionsStore>,
                TransferInput<ReachingDefinitionsValue, ReachingDefinitionsStore>>
        implements ForwardTransferFunction<ReachingDefinitionsValue, ReachingDefinitionsStore> {

    @Override
    public ReachingDefinitionsStore initialStore(
            UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
        return new ReachingDefinitionsStore();
    }

    @Override
    public RegularTransferResult<ReachingDefinitionsValue, ReachingDefinitionsStore> visitNode(
            Node n, TransferInput<ReachingDefinitionsValue, ReachingDefinitionsStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<ReachingDefinitionsValue, ReachingDefinitionsStore>
            visitAssignment(
                    AssignmentNode n,
                    TransferInput<ReachingDefinitionsValue, ReachingDefinitionsStore> p) {
        RegularTransferResult<ReachingDefinitionsValue, ReachingDefinitionsStore> transferResult =
                (RegularTransferResult<ReachingDefinitionsValue, ReachingDefinitionsStore>)
                        super.visitAssignment(n, p);
        processDefinition(n, transferResult.getRegularStore());
        return transferResult;
    }

    /**
     * Update the information of reaching definitions from an assignment statement.
     *
     * @param def the definition that should be put into the store
     * @param store the reaching definitions store
     */
    private void processDefinition(AssignmentNode def, ReachingDefinitionsStore store) {
        store.killDef(def.getTarget());
        store.putDef(new ReachingDefinitionsValue(def));
    }
}
