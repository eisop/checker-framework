package common;

import org.checkerframework.dataflow.analysis.AbstractValue;
import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.analysis.Store;
import org.checkerframework.dataflow.analysis.TransferFunction;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class DataflowTester {

    /**
     * The performTest method performs the tests for some dataflow analysis.
     *
     * @param analysis instance of forward or backward analysis.
     */
    public static <V extends AbstractValue<V>, S extends Store<S>, T extends TransferFunction<V, S>>
            void performTest(Analysis<V, S, T> analysis) {

        String inputFile = "Test.java";
        String method = "test";
        String clazz = "Test";
        String outputFile = "Out.txt";

        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        Map<String, Object> res =
                cfgVisualizeLauncher.generateStringOfCFG(inputFile, method, clazz, true, analysis);
        try (FileWriter out = new FileWriter(outputFile)) {
            out.write(res.get("stringGraph").toString());
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
