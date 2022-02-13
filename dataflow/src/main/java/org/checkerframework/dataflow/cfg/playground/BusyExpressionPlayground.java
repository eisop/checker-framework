package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.busyexpression.BusyExprStore;
import org.checkerframework.dataflow.busyexpression.BusyExprTransfer;
import org.checkerframework.dataflow.busyexpression.BusyExprValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

public class BusyExpressionPlayground {
    public static void main(String[] args) {

        /* Configuration: change as appropriate */
        String inputFile =
                "dataflow/manual/examples/BusyExprSimple.java"; // input file name and path
        String outputDir = "."; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // Run the analysis and create a PDF file
        BusyExprTransfer transfer = new BusyExprTransfer();
        BackwardAnalysis<BusyExprValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        cfgVisualizeLauncher.generateDOTofCFG(
                inputFile, outputDir, method, clazz, true, true, backwardAnalysis);
    }
}
