import org.checkerframework.checker.pico.qual.Readonly;

// Done
public class EvilUnionFind {
    private int size;
    private int[] pointsTo;

    public EvilUnionFind(int n) {
        this.size = n;
        this.pointsTo = new int[n];
        for (int i = 0; i < n; i++) {
            this.pointsTo[i] = -1;
        }
    }

    public void union(int a, int b) {
        if (this.pointsTo[a] == -1) {
            // pass
        } else {
            while (this.pointsTo[this.pointsTo[a]] != -1) {
                this.pointsTo[a] = this.pointsTo[this.pointsTo[a]];
            }
            a = this.pointsTo[a];
        }

        if (a == b) {
            return;
        }
        this.pointsTo[a] = b;
    }

    // @viewmethod
    public int find(@Readonly EvilUnionFind this, int a) {
        if (this.pointsTo[a] == -1) {
            return a;
        } else {
            while (this.pointsTo[this.pointsTo[a]] != -1) {
                // :: error: (illegal.array.write)
                this.pointsTo[a] = this.pointsTo[this.pointsTo[a + 1]]; // the +1 is evil
            }
            return this.pointsTo[a];
        }
    }
}
