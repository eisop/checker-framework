package org.checkerframework.dataflow.util;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * It's basically a class that provides utility for running some dataflow analysis tests and
 * playgrounds.
 */
public class DataflowPerformer {

    /**
     * The performTest method performs the tests for some dataflow analysis.
     *
     * @param analysis instance of forward or backward analysis from specific dataflow test case.
     */
    @SuppressWarnings("CatchAndPrintStackTrace") // we want to use e.printStackTrace here.
    public static void performTest(Analysis<?, ?, ?> analysis) {
        String inputFile = "Test.java";
        String method = "test";
        String clazz = "Test";
        String outputFile = "Out.txt";

        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        Map<String, Object> res =
                cfgVisualizeLauncher.generateStringOfCFG(inputFile, method, clazz, true, analysis);
        try (FileWriter out = new FileWriter(outputFile)) {
            if (res != null && res.get("stringGraph") != null) {
                out.write(res.get("stringGraph").toString());
            }
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The performPlayground method performs the playgrounds for some dataflow analysis. In order to
     * run this method successfully, make sure the settings of inputFile and outputDir are correct.
     *
     * @param analysis instance of forward or backward analysis from specific dataflow playground..
     */
    public static void performPlayground(Analysis<?, ?, ?> analysis) {

        /* Configuration: change as appropriate */
        String inputFile = "Test.java"; // input file name and path
        String outputDir = "cfg"; // output directory
        String method = "test"; // name of the method to analyze
        String clazz = "Test"; // name of the class to consider

        // Run the analysis and create a PDF file
        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        cfgVisualizeLauncher.generateDOTofCFG(
                inputFile, outputDir, method, clazz, true, true, analysis);
    }
}
