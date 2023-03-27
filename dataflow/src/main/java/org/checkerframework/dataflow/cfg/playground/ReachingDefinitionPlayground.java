package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeOptions;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionStore;
import org.checkerframework.dataflow.reachingdef.ReachingDefinitionTransfer;

/** The playground of reaching definition analysis. */
public class ReachingDefinitionPlayground {
    /**
     * Run reaching definition analysis for a specific file and create a PDF of the CFG in the end.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {

        // Parse the arguments.
        CFGVisualizeOptions config = CFGVisualizeOptions.parseArgs(args);

        // Run the analysis and create a PDF file
        ReachingDefinitionTransfer transfer = new ReachingDefinitionTransfer();
        ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionStore, ReachingDefinitionTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.performAnalysis(config, forwardAnalysis);
    }
}
