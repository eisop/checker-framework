package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.constantpropagation.Constant;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationStore;
import org.checkerframework.dataflow.constantpropagation.ConstantPropagationTransfer;

/** The playground for constant propagation analysis. */
public class ConstantPropagationPlayground {

    /** Do not instantiate. */
    private ConstantPropagationPlayground() {
        throw new Error("do not instantiate");
    }

    /**
     * Run constant propagation analysis on a file.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        // Parse the arguments.
        CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

        // run the analysis and create a PDF file
        ConstantPropagationTransfer transfer = new ConstantPropagationTransfer();
        ForwardAnalysis<Constant, ConstantPropagationStore, ConstantPropagationTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.performAnalysis(config, forwardAnalysis);
    }
}
