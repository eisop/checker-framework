package org.checkerframework.checker.mutability;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.ConditionalTransferResult;
import org.checkerframework.dataflow.analysis.RegularTransferResult;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.cfg.node.NullLiteralNode;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/** The transfer function for the mutability type system. */
public class MutabilityNoInitTransfer
        extends CFAbstractTransfer<
                MutabilityNoInitValue, MutabilityNoInitStore, MutabilityNoInitTransfer> {
    /** The mutability type factory. */
    private final MutabilityNoInitAnnotatedTypeFactory mutabilityTypeFactory;

    /**
     * Create a new MutabilityNoInitTransfer.
     *
     * @param analysis the analysis
     */
    public MutabilityNoInitTransfer(MutabilityNoInitAnalysis analysis) {
        super(analysis);
        mutabilityTypeFactory = (MutabilityNoInitAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    @Override
    public TransferResult<MutabilityNoInitValue, MutabilityNoInitStore> visitAssignment(
            AssignmentNode n, TransferInput<MutabilityNoInitValue, MutabilityNoInitStore> in) {
        if (n.getExpression() instanceof NullLiteralNode
                && n.getTarget().getTree() instanceof VariableTree) {
            VariableElement varElement =
                    TreeUtils.elementFromDeclaration((VariableTree) n.getTarget().getTree());
            // Do not refine a field to @Bottom from a null initializer. That refinement causes
            // false positive illegal-write errors for fields initialized to null, but assignments
            // to fields in methods or constructors are still refined normally.
            if (varElement != null && varElement.getKind().isField()) {
                MutabilityNoInitStore store = in.getRegularStore();
                MutabilityNoInitValue storeValue = in.getValueOfSubNode(n);
                MutabilityNoInitValue value = moreSpecificValue(null, storeValue);
                return new RegularTransferResult<>(finishValue(value, store), store);
            }
        }
        return super.visitAssignment(n, in);
    }

    @Override
    protected TransferResult<MutabilityNoInitValue, MutabilityNoInitStore>
            strengthenAnnotationOfEqualTo(
                    TransferResult<MutabilityNoInitValue, MutabilityNoInitStore> res,
                    Node firstNode,
                    Node secondNode,
                    MutabilityNoInitValue firstValue,
                    MutabilityNoInitValue secondValue,
                    boolean notEqualTo) {
        res =
                super.strengthenAnnotationOfEqualTo(
                        res, firstNode, secondNode, firstValue, secondValue, notEqualTo);

        AnnotationMirror classBound = getClassBoundFromClassLiteral(firstNode);
        Node getClassReceiver = getGetClassReceiver(secondNode);
        if (classBound == null || getClassReceiver == null) {
            return res;
        }

        MutabilityNoInitStore thenStore = res.getThenStore();
        MutabilityNoInitStore elseStore = res.getElseStore();
        boolean refined = false;
        for (Node receiverPart : splitAssignments(getClassReceiver)) {
            JavaExpression receiverExpression = JavaExpression.fromNode(receiverPart);
            if (!receiverExpression.isDeterministic(mutabilityTypeFactory)
                    || !CFAbstractStore.canInsertJavaExpression(receiverExpression)) {
                continue;
            }
            if (notEqualTo) {
                elseStore.insertValue(receiverExpression, classBound);
            } else {
                thenStore.insertValue(receiverExpression, classBound);
            }
            refined = true;
        }

        if (!refined) {
            return res;
        }
        return new ConditionalTransferResult<>(res.getResultValue(), thenStore, elseStore);
    }

    /**
     * Returns the mutability declaration bound for a class literal, or null if {@code node} is not
     * a class literal.
     *
     * @param node a node to inspect
     * @return the mutability declaration bound for the class literal
     */
    private @Nullable AnnotationMirror getClassBoundFromClassLiteral(Node node) {
        Tree tree = node.getTree();
        if (!TreeUtils.isClassLiteral(tree)) {
            return null;
        }
        ExpressionTree classLiteralType = ((MemberSelectTree) tree).getExpression();
        TypeMirror classType = TreeUtils.typeOf(classLiteralType);
        AnnotationMirrorSet bounds = mutabilityTypeFactory.getTypeDeclarationBounds(classType);
        return mutabilityTypeFactory
                .getQualifierHierarchy()
                .findAnnotationInHierarchy(bounds, mutabilityTypeFactory.READONLY);
    }

    /**
     * Returns the receiver of a {@code getClass()} invocation, or null if {@code node} is not such
     * an invocation.
     *
     * @param node a node to inspect
     * @return the {@code getClass()} receiver
     */
    private @Nullable Node getGetClassReceiver(Node node) {
        if (!(node instanceof MethodInvocationNode)) {
            return null;
        }
        MethodInvocationNode methodInvocation = (MethodInvocationNode) node;
        ExecutableElement method = methodInvocation.getTarget().getMethod();
        if (method.getSimpleName().contentEquals("getClass") && method.getParameters().isEmpty()) {
            return methodInvocation.getTarget().getReceiver();
        }
        return null;
    }
}
