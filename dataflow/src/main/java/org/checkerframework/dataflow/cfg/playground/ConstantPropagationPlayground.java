package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** The playground of constant propagation analysis. */
public class ConstantPropagationPlayground {

    /**
     * Run constant propagation for a specific file and create a PDF of the CFG in the end.
     *
     * @param args input arguments, not used
     */
    public static void main(String[] args) {
        // run the analysis and create a PDF file
        ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
        ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.generateDOTofCFGforPlayground(forwardAnalysis);
    }
}
