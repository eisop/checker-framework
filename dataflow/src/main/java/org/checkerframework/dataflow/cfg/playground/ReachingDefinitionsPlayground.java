package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.reachingdefinitions.ReachingDefinitionsStore;
import org.checkerframework.dataflow.reachingdefinitions.ReachingDefinitionsTransfer;

/** The playground of reaching definitions analysis. */
public class ReachingDefinitionsPlayground {
    /**
     * Run reaching definitions analysis for a specific file and create a PDF of the CFG in the end.
     *
     * @param args input arguments, not used
     */
    public static void main(String[] args) {

        /* Configuration: change as appropriate */
        String inputFile = "Test.java"; // input file name and path
        String outputDir = "cfg"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // Run the analysis and create a PDF file
        ReachingDefinitionsTransfer transfer = new ReachingDefinitionsTransfer();
        ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionsStore, ReachingDefinitionsTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.generateDOTofCFG(
                inputFile, outputDir, method, clazz, true, true, forwardAnalysis);
    }
}
