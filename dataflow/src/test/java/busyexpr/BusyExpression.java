package busyexpr;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.busyexpression.BusyExprStore;
import org.checkerframework.dataflow.busyexpression.BusyExprTransfer;
import org.checkerframework.dataflow.busyexpression.BusyExprValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

/** Used in busyExpressionTest Gradle task to test the BusyExpression analysis. */
public class BusyExpression {
    /**
     * The main method expects to be run in dataflow/tests/busy-expression directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        String inputFile = "dataflow/tests/busy-expression/Test.java"; // input file name;
        String method = "test";
        String clazz = "Test";
        String outputFile = "Out.txt";

        BusyExprTransfer transfer = new BusyExprTransfer();
        BackwardAnalysis<BusyExprValue, BusyExprStore, BusyExprTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.writeStringOfCFG(
                inputFile, method, clazz, outputFile, backwardAnalysis);
    }
}
