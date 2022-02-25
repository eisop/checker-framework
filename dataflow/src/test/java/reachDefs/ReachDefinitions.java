package reachDefs;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.reachdefinitions.ReachDefinitionsStore;
import org.checkerframework.dataflow.reachdefinitions.ReachDefinitionsTransfer;
import org.checkerframework.dataflow.reachdefinitions.ReachDefinitionsValue;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/** Used in liveVariableTest Gradle task to test the LiveVariable analysis. */
public class ReachDefinitions {

    /**
     * The main method expects to be run in dataflow/tests/live-variable directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        String inputFile = "Test.java";
        String method = "test";
        String clazz = "Test";
        String outputFile = "Out.txt";

        ReachDefinitionsTransfer transfer = new ReachDefinitionsTransfer();
        ForwardAnalysis<ReachDefinitionsValue, ReachDefinitionsStore, ReachDefinitionsTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher cfgVisualizeLauncher = new CFGVisualizeLauncher();
        Map<String, Object> res =
                cfgVisualizeLauncher.generateStringOfCFG(
                        inputFile, method, clazz, true, forwardAnalysis);
        try (FileWriter out = new FileWriter(outputFile)) {
            out.write(res.get("stringGraph").toString());
            out.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
