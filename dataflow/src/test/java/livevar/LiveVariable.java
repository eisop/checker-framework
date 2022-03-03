package livevar;

import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;
import org.checkerframework.dataflow.livevariable.LiveVarValue;

/** Used in liveVariableTest Gradle task to test the LiveVariable analysis. */
public class LiveVariable {

    /**
     * The main method expects to be run in dataflow/tests/live-variable directory.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        LiveVarTransfer transfer = new LiveVarTransfer();
        BackwardAnalysis<LiveVarValue, LiveVarStore, LiveVarTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        common.PerformDFATest.performTest(backwardAnalysis);
    }
}
