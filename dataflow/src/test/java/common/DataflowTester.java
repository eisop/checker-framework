package common;

import org.checkerframework.dataflow.analysis.Analysis;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * A common tester performs dataflow tests.
 *
 * <p>A DataflowTester receive the {@link Analysis} instance from specific dataflow test, e.g.,
 * {@link livevar.LiveVariable}, then generate CFG by using this instance. In order to test the
 * correctness of the analysis, performTest reads from the input file inputFile and then generates
 * an output file of CFG, which will be compared with an expectation output file under the working
 * dir.
 */
public class DataflowTester {

    /**
     * The performTest method performs the tests for some dataflow analysis.
     *
     * @param analysis instance of forward or backward analysis.
     */
    public static void performTest(Analysis<?, ?, ?> analysis) {

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
