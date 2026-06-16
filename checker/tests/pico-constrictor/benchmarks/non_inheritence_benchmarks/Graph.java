import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Done
public class Graph {
    private int nodeCount;
    private @Mutable List<Integer> edgeCounts;
    private Map<Integer, List<Integer>> edges;

    public Graph() {
        this.nodeCount = 0;
        this.edgeCounts = new ArrayList<>();
        this.edges = new HashMap<>();
    }

    public int addNode() {
        int ret = this.nodeCount;
        this.nodeCount += 1;
        while (edgeCounts.size() <= this.nodeCount) {
            edgeCounts.add(0);
        }
        edgeCounts.set(this.nodeCount, 0);
        return ret;
    }

    // @immutable - violation
    public void addEdge(@Readonly Graph this, int node1, int node2) {
        if (!this.edges.containsKey(node1)) {
            // :: error: (method.invocation.invalid)
            this.edges.put(node1, new ArrayList<>());
        }
        List<Integer> list = this.edges.get(node1);
        int currentSize = this.edgeCounts.get(node1);
        while (list.size() <= currentSize) {
            list.add(0);
        }
        list.set(currentSize, node2);
        // :: error: (method.invocation.invalid)
        this.edges.put(node1, list);
        this.edgeCounts.set(node1, this.edgeCounts.get(node1) + 1);
    }

    // @viewmethod
    public boolean hasEdge(int node1, int node2) {
        if (!this.edges.containsKey(node1)) {
            return false;
        }
        List<Integer> adj = this.edges.get(node1);
        for (int node : adj) {
            if (node == node2) {
                return true;
            }
        }
        return false;
    }
}
