package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.busyexpr.BusyExprStore;
import org.checkerframework.dataflow.busyexpr.BusyExprTransfer;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;

/** The playground for busy expression analysis */
public class BusyExpressionPlayground {

    /**
     * Run busy expression analysis playground on a test file and print the CFG graph
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        // Parse the arguments.
        CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

        // Run the analysis and create a PDF file
        BusyExprTransfer transfer = new BusyExprTransfer();
        BackwardAnalysis<UnusedAbstractValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.performAnalysis(config, backwardAnalysis);
    }
}
