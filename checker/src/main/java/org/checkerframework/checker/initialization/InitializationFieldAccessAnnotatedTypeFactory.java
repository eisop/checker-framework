package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.UnderlyingAST.CFGMethod;
import org.checkerframework.dataflow.cfg.node.AssignmentNode;
import org.checkerframework.dataflow.cfg.node.LocalVariableNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.flow.CFAbstractAnalysis.FieldInitialValue;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.flow.FlowAnalysisRelevance;
import org.checkerframework.javacutil.AnnotationMirrorSet;

import java.util.IdentityHashMap;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** The type factory for the {@link InitializationFieldAccessSubchecker}. */
public class InitializationFieldAccessAnnotatedTypeFactory
        extends InitializationParentAnnotatedTypeFactory {

    /**
     * Decides whether the analysis of a method can compute a value other than {@code @Initialized}.
     */
    private final FlowAnalysisRelevance<CFValue> flowRelevance;

    /**
     * Create a new InitializationFieldAccessAnnotatedTypeFactory.
     *
     * @param checker the checker to which the new type factory belongs
     */
    @SuppressWarnings("this-escape")
    public InitializationFieldAccessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        flowRelevance =
                new FlowAnalysisRelevance<CFValue>(this) {
                    @Override
                    protected boolean isIrrelevantQualifier(AnnotationMirror anno) {
                        return isInitialized(anno);
                    }
                };
        postInit();
    }

    @Override
    protected boolean isFlowAnalysisRedundant(
            CFGMethod method, List<FieldInitialValue<CFValue>> fieldValues) {
        return flowRelevance.isFlowAnalysisRedundant(method, fieldValues);
    }

    /**
     * In a method whose analysis is redundant, the analysis computes {@code @Initialized} for every
     * local variable and every assignment to one. Their declared type is the top type, so the type
     * factory needs these values. Nodes in dead code get no value, as in the analysis.
     */
    @Override
    protected void addRedundantFlowAnalysisValues(
            ControlFlowGraph cfg, IdentityHashMap<Node, CFValue> nodeValues) {
        for (Node node : cfg.getAllNodes(this::isIgnoredExceptionType)) {
            if (node instanceof LocalVariableNode
                    || (node instanceof AssignmentNode
                            && ((AssignmentNode) node).getTarget() instanceof LocalVariableNode)) {
                TypeMirror type = node.getType();
                // A type variable has no primary qualifier; its bounds are @Initialized.
                CFValue value =
                        type.getKind() == TypeKind.TYPEVAR
                                ? analysis.createAbstractValue(AnnotationMirrorSet.emptySet(), type)
                                : analysis.createSingleAnnotationValue(INITIALIZED, type);
                if (value != null) {
                    nodeValues.put(node, value);
                }
            }
        }
    }

    @Override
    protected InitializationAnalysis createFlowAnalysis() {
        return new InitializationAnalysis(checker, this);
    }

    /**
     * Runs the analysis only if initialization checking is turned on. This overrides the
     * implementation method and not {@link #performFlowAnalysisForClass(ClassTree)}, which does the
     * bookkeeping that every factory needs and is final.
     */
    @Override
    protected void performFlowAnalysisForClassImpl(ClassTree classTree) {
        // Only perform the analysis if initialization checking is turned on.
        if (!assumeInitialized) {
            super.performFlowAnalysisForClassImpl(classTree);
        }
    }

    /**
     * Returns the flow analysis.
     *
     * @return the flow analysis
     * @see #getFlowResult()
     */
    /*package-private*/ InitializationAnalysis getAnalysis() {
        return analysis;
    }

    /**
     * Returns the result of the flow analysis. Invariant:
     *
     * <pre>
     *  scannedClasses.get(c) == FINISHED for some class c &rArr; flowResult != null
     * </pre>
     *
     * Note that flowResult contains analysis results for Trees from multiple classes which are
     * produced by multiple calls to performFlowAnalysisForClass.
     *
     * @return the result of the flow analysis
     * @see #getAnalysis()
     */
    /*package-private*/ AnalysisResult<CFValue, InitializationStore> getFlowResult() {
        return flowResult;
    }
}
