// Regression test for an aliasing bug in AbstractAnalysis.setNodeValues.
//
// AnalysisResult wraps the analysis's `nodeValues` map in an UnmodifiableIdentityHashMap
// (a read-through view, not a copy) and passes it back into the analysis through
// getStoreAfter(Block) -> runAnalysisFor -> setNodeValues.  The old implementation did
// `nodeValues.clear(); nodeValues.putAll(in)`, which cleared the very map the view reads,
// wiping every node value; the fix copies before mutating (`new IdentityHashMap<>(in)`).
//
// getStoreAfter(Block) is only reached with verbose CFG visualization, and only the blocks
// visualized *after* the first one lose their values (the first block is rendered before the
// wipe).  This test therefore needs a method with more than one node-bearing block (the
// if/else below) and is compiled with `-Acfgviz=...StringCFGVisualizer,verbose`.  With the
// bug present, the abstract values on the `b = a`, `b = 4`, and `return b` nodes disappear
// from the visualized output; with the fix they are present.  See Expected.out.
public class CfgVizNodeValues {
    int m(boolean cond) {
        int a = 0;
        int b;
        if (cond) {
            b = a;
        } else {
            b = 4;
        }
        return b;
    }
}
