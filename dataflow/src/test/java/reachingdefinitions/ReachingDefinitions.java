package reachingdefinitions;

import org.checkerframework.dataflow.analysis.ForwardAnalysis;
import org.checkerframework.dataflow.analysis.ForwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.reachingdefinitions.ReachingDefinitionsStore;
import org.checkerframework.dataflow.reachingdefinitions.ReachingDefinitionsTransfer;

/** Used in reachingDefinitionsTest Gradle task to test the ReachingDefinitions analysis. */
public class ReachingDefinitions {

    /**
     * The main method expects to be run in dataflow/tests/reaching-definitions directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {

        String inputFile = "Test.java";
        String method = "test";
        String clas = "Test";
        String outputFile = "Out.txt";

        ReachingDefinitionsTransfer transfer = new ReachingDefinitionsTransfer();
        ForwardAnalysis<UnusedAbstractValue, ReachingDefinitionsStore, ReachingDefinitionsTransfer>
                forwardAnalysis = new ForwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.writeStringOfCFG(inputFile, method, clas, outputFile, forwardAnalysis);
    }
}
