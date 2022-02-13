package org.checkerframework.dataflow.busyexpression;

import org.checkerframework.dataflow.analysis.BackwardTransferFunction;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.UnderlyingAST;
import org.checkerframework.dataflow.cfg.node.*;

import java.util.List;

/** A busy expression transfer function */
public class BusyExprTransfer
        extends AbstractNodeVisitor<
                TransferResult<BusyExprValue, BusyExprStore>,
                TransferInput<BusyExprValue, BusyExprStore>>
        implements BackwardTransferFunction<BusyExprValue, BusyExprStore> {

    @Override
    public BusyExprStore initialNormalExitStore(
            UnderlyingAST underlyingAST, List<ReturnNode> returnNodes) {
        return new BusyExprStore();
    }

    @Override
    public BusyExprStore initialExceptionalExitStore(UnderlyingAST underlyingAST) {
        return new BusyExprStore();
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitNode(
            Node n, TransferInput<BusyExprValue, BusyExprStore> p) {
        return new RegularTransferResult<>(null, p.getRegularStore());
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitAssignment(
            AssignmentNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
        RegularTransferResult<BusyExprValue, BusyExprStore> transferResult =
                (RegularTransferResult<BusyExprValue, BusyExprStore>) super.visitAssignment(n, p);
        processBusyExprInAssignment(
                n.getTarget(), n.getExpression(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitStringConcatenateAssignment(
            StringConcatenateAssignmentNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
        RegularTransferResult<BusyExprValue, BusyExprStore> transferResult =
                (RegularTransferResult<BusyExprValue, BusyExprStore>)
                        super.visitStringConcatenateAssignment(n, p);
        processBusyExprInAssignment(
                n.getLeftOperand(), n.getRightOperand(), transferResult.getRegularStore());
        return transferResult;
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
        RegularTransferResult<BusyExprValue, BusyExprStore> transferResult =
                (RegularTransferResult<BusyExprValue, BusyExprStore>)
                        super.visitMethodInvocation(n, p);
        BusyExprStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitObjectCreation(
            ObjectCreationNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
        RegularTransferResult<BusyExprValue, BusyExprStore> transferResult =
                (RegularTransferResult<BusyExprValue, BusyExprStore>)
                        super.visitObjectCreation(n, p);
        BusyExprStore store = transferResult.getRegularStore();
        for (Node arg : n.getArguments()) {
            store.addUseInExpression(arg);
        }
        return transferResult;
    }

    @Override
    public RegularTransferResult<BusyExprValue, BusyExprStore> visitReturn(
            ReturnNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
        RegularTransferResult<BusyExprValue, BusyExprStore> transferResult =
                (RegularTransferResult<BusyExprValue, BusyExprStore>) super.visitReturn(n, p);
        Node result = n.getResult();
        if (result != null) {
            BusyExprStore store = transferResult.getRegularStore();
            store.addUseInExpression(result);
        }
        return transferResult;
    }

    //    @Override
    //    public RegularTransferResult<BusyExprValue, BusyExprStore> visitVariableDeclaration(
    //            VariableDeclarationNode n, TransferInput<BusyExprValue, BusyExprStore> p) {
    //        return null;
    //    }
    /**
     * Update the information of busy expression store from an assignment statement.
     *
     * @param variable if any expression has this variable, that expression should be killed
     * @param expression the expression should be added
     * @param store the busy expression store
     */
    public void processBusyExprInAssignment(Node variable, Node expression, BusyExprStore store) {
        store.killBusyExpr(variable);
        store.addUseInExpression(expression);
    }
}
