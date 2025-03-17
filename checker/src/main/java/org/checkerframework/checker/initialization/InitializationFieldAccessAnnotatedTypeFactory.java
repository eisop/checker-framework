package org.checkerframework.checker.initialization;

import com.sun.source.tree.ClassTree;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.AnalysisResult;
import org.checkerframework.framework.flow.CFValue;

/** The type factory for the {@link InitializationFieldAccessSubchecker}. */
public class InitializationFieldAccessAnnotatedTypeFactory
        extends InitializationParentAnnotatedTypeFactory {

    /**
     * Create a new InitializationFieldAccessAnnotatedTypeFactory.
     *
     * @param checker the checker to which the new type factory belongs
     */
    public InitializationFieldAccessAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        postInit();
    }

    @Override
    protected InitializationAnalysis createFlowAnalysis() {
        return new InitializationAnalysis(checker, this);
    }

    @Override
    protected void performFlowAnalysis(ClassTree classTree) {
        // Only perform the analysis if initialization checking is turned on.
        if (!assumeInitialized) {
            super.performFlowAnalysis(classTree);
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
     * produced by multiple calls to performFlowAnalysis.
     *
     * @return the result of the flow analysis
     * @see #getAnalysis()
     */
    /*package-private*/ AnalysisResult<CFValue, InitializationStore> getFlowResult() {
        return flowResult;
    }
}
