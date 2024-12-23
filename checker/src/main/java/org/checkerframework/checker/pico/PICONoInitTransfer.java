package org.checkerframework.checker.pico;

import com.sun.source.tree.VariableTree;

import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.VariableElement;

/** The transfer function for the immutability type system. */
public class PICONoInitTransfer
        extends CFAbstractTransfer<PICONoInitValue, PICONoInitStore, PICONoInitTransfer> {
    /**
     * Create a new PICONoInitTransfer.
     *
     * @param analysis the analysis
     */
    public PICONoInitTransfer(PICONoInitAnalysis analysis) {
        super(analysis);
    }

    @Override
    public TransferResult<PICONoInitValue, PICONoInitStore> visitAssignment(
            AssignmentNode n, TransferInput<PICONoInitValue, PICONoInitStore> in) {
        if (n.getExpression() instanceof NullLiteralNode
                && n.getTarget().getTree() instanceof VariableTree) {
            VariableElement varElement =
                    TreeUtils.elementFromDeclaration((VariableTree) n.getTarget().getTree());
            // Below is for removing false positive warning of bottom illegal write cacused by
            // refining field to @Bottom if
            // field initializer is null.
            // Forbid refinement from null literal in initializer to fields variable tree(identifier
            // tree not affected, e.g.
            // assigning a field as null in instance methods or constructors)
            if (varElement != null && varElement.getKind().isField()) {
                PICONoInitStore store = in.getRegularStore();
                PICONoInitValue storeValue = in.getValueOfSubNode(n);
                PICONoInitValue value = moreSpecificValue(null, storeValue);
                return new RegularTransferResult<>(finishValue(value, store), store);
            }
        }
        return super.visitAssignment(n, in);
    }
}
