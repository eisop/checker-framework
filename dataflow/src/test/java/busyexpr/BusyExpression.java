package busyexpr;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.busyexpression.BusyExprStore;
import org.checkerframework.dataflow.busyexpression.BusyExprTransfer;
import org.checkerframework.dataflow.busyexpression.BusyExprValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/** Used in busyExpressionTest Gradle task to test the BusyExpression analysis. */
public class BusyExpression {
    /**
     * The main method expects to be run in dataflow/tests/busy-expression directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        String inputFile = "Test.java";
        String method = "test";
        String clazz = "Test";
        String outputFile = "Out.txt";

        BusyExprTransfer transfer = new BusyExprTransfer();
        BackwardAnalysis<BusyExprValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        Map<String, Object> res =
                cfgVisualizeLauncher.generateStringOfCFG(
                        inputFile, method, clazz, true, backwardAnalysis);
        try (FileWriter out = new FileWriter(outputFile)) {
            out.write(res.get("stringGraph").toString());
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
