package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;
import org.checkerframework.dataflow.livevariable.LiveVarValue;

/** The playground of live variable analysis. */
public class LiveVariablePlayground {

    /**
     * Run live variable analysis for a specific file and create a PDF of the CFG in the end.
     *
     * @param args input arguments, not used
     */
    public static void main(String[] args) {
        // Run the analysis and create a PDF file
        LiveVarTransfer transfer = new LiveVarTransfer();
        BackwardAnalysis<LiveVarValue, LiveVarStore, LiveVarTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.generateDOTofCFGforPlayground(backwardAnalysis);
    }
}
