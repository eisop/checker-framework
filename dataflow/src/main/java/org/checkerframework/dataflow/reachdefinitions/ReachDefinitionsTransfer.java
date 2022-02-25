package org.checkerframework.dataflow.reachdefinitions;

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

/** A reach definition transfer function. */
public class ReachDefinitionsTransfer
        extends AbstractNodeVisitor<
                TransferResult<ReachDefinitionsValue, ReachDefinitionsStore>,
                TransferInput<ReachDefinitionsValue, ReachDefinitionsStore>>
        implements ForwardTransferFunction<ReachDefinitionsValue, ReachDefinitionsStore> {

    @Override
    public ReachDefinitionsStore initialStore(
            UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
        return new ReachDefinitionsStore();
    }

    @Override
    public RegularTransferResult<ReachDefinitionsValue, ReachDefinitionsStore> visitNode(
            Node n, TransferInput<ReachDefinitionsValue, ReachDefinitionsStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<ReachDefinitionsValue, ReachDefinitionsStore> visitAssignment(
            AssignmentNode n, TransferInput<ReachDefinitionsValue, ReachDefinitionsStore> p) {
        RegularTransferResult<ReachDefinitionsValue, ReachDefinitionsStore> transferResult =
                (RegularTransferResult<ReachDefinitionsValue, ReachDefinitionsStore>)
                        super.visitAssignment(n, p);
        processDefinition(n, transferResult.getRegularStore());
        return transferResult;
    }

    /**
     * Update the information of reach definition from an assignment statement.
     *
     * @param def the definition that should be put into the store
     * @param store the reach definition store
     */
    private void processDefinition(AssignmentNode def, ReachDefinitionsStore store) {
        store.killDef(def.getTarget());
        store.putDef(new ReachDefinitionsValue(def));
    }
}
