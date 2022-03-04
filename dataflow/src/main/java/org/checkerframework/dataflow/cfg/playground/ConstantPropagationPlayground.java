package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;
import org.checkerframework.dataflow.util.DataflowTester;

public class ConstantPropagationPlayground {

    /** Run constant propagation for a specific file and create a PDF of the CFG in the end. */
    public static void main(String[] args) {
        // run the analysis and create a PDF file
        ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
        ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        DataflowTester.performPlayground(forwardAnalysis);
    }
}
