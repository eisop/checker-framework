package cfgconstruction;

import org.checkerframework.dataflow.cfg.ControlFlowGraph;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;

/** Builds the control flow graph of a method of a test file and generates the visualization. */
public class CFGConstruction {

    /** Do not instantiate. */
    private CFGConstruction() {
        throw new Error("Do not instantiate");
    }

    /**
     * Builds and visualizes a control flow graph.
     *
     * @param args the command-line arguments, which are ignored
     */
    public static void main(String[] args) {
        String inputFile = "Test.java";
        String clazz = "Test";
        String method = "manyNestedTryFinallyBlocks";

        ControlFlowGraph cfg =
                CFGVisualizeLauncher.generateMethodCFG(
                        inputFile, method, clazz, /* analysis= */ null);
        cfg.checkInvariants();
    }
}
