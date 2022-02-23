package org.checkerframework.dataflow.reachdefinition;

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
public class ReachDefTransfer
        extends AbstractNodeVisitor<
                TransferResult<ReachDefinitionValue, ReachDefinitionStore>,
                TransferInput<ReachDefinitionValue, ReachDefinitionStore>>
        implements ForwardTransferFunction<ReachDefinitionValue, ReachDefinitionStore> {

    @Override
    public ReachDefinitionStore initialStore(
            UnderlyingAST underlyingAST, @Nullable List<LocalVariableNode> parameters) {
        return new ReachDefinitionStore();
    }

    @Override
    public RegularTransferResult<ReachDefinitionValue, ReachDefinitionStore> visitNode(
            Node n, TransferInput<ReachDefinitionValue, ReachDefinitionStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<ReachDefinitionValue, ReachDefinitionStore> visitAssignment(
            AssignmentNode n, TransferInput<ReachDefinitionValue, ReachDefinitionStore> p) {
        RegularTransferResult<ReachDefinitionValue, ReachDefinitionStore> transferResult =
                (RegularTransferResult<ReachDefinitionValue, ReachDefinitionStore>)
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
    private void processDefinition(AssignmentNode def, ReachDefinitionStore store) {
        store.killDef(def.getTarget());
        store.putDef(new ReachDefinitionValue(def));
    }
}
