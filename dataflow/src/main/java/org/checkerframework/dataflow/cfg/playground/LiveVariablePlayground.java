package org.checkerframework.dataflow.cfg.playground;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.analysis.BackwardAnalysis;
import org.checkerframework.dataflow.analysis.BackwardAnalysisImpl;
import org.checkerframework.dataflow.analysis.UnusedAbstractValue;
import org.checkerframework.dataflow.cfg.visualize.CFGVisualizeLauncher;
import org.checkerframework.dataflow.livevariable.LiveVarStore;
import org.checkerframework.dataflow.livevariable.LiveVarTransfer;

import java.io.File;

/** The playground of live variable analysis. */
public class LiveVariablePlayground {

    /**
     * Run live variable analysis for a specific file and create a PDF of the CFG in the end.
     *
     * @param args command-line arguments, not used
     */
    public static void main(String[] args) {
	if (args.length == 0) {
		printUsage();
		System.exit(1);
	}
	String input = args[0];
	File file = new File(input);
	if (!file.canRead()) {
		printError("Cannot read input file: " + file.getAbsolutePath());
		printUsage();
		System.exit(1);
	}

	String method = "test";
	String clas = "Test";
	String output = ".";
	boolean pdf = false;
	boolean error = false;
	boolean verbose = false;

	for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--outputdir":
                    if (i >= args.length - 1) {
                        printError("Did not find <outputdir> after --outputdir.");
                        continue;
                    }
                    i++;
                    output = args[i];
                    break;
                case "--pdf":
                    pdf = true;
                    break;
                case "--method":
                    if (i >= args.length - 1) {
                        printError("Did not find <name> after --method.");
                        continue;
                    }
                    i++;
                    method = args[i];
                    break;
                case "--class":
                    if (i >= args.length - 1) {
                        printError("Did not find <name> after --class.");
                        continue;
                    }
                    i++;
                    clas = args[i];
                    break;
                case "--verbose":
                    verbose = true;
                    break;
                default:
                    printError("Unknown command line argument: " + args[i]);
                    error = true;
                    break;
            }
        }
	

        if (error) {
		System.exit(1);
	}
	
        LiveVarTransfer transfer = new LiveVarTransfer();
        BackwardAnalysis<UnusedAbstractValue, LiveVarStore, LiveVarTransfer> backwardAnalysis =
                new BackwardAnalysisImpl<>(transfer);
        CFGVisualizeLauncher.generateDOTofCFG(
                input, output, method, clas, pdf, verbose, backwardAnalysis);
    }

    private static void printUsage() {
	System.out.println(
                "Parameters: <inputfile> [--outputdir <outputdir>] [--method <name>] [--class"
                        + " <name>] [--pdf] [--verbose] [--string]");
        System.out.println(
                "    --outputdir: The output directory for the generated files (defaults to '.').");
    }

    private static void printError(@Nullable String string) {
        System.err.println("ERROR: " + string);
    }
}
