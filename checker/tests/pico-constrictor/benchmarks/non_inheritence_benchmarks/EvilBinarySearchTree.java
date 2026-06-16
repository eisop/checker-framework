import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.List;

public class EvilBinarySearchTree {
    private int root;
    private List<Integer> lefts;
    private List<Integer> rights;
    private List<Integer> values;
    private int nodes;

    public EvilBinarySearchTree() {
        this.root = -1;
        this.lefts = new ArrayList<>();
        this.rights = new ArrayList<>();
        this.values = new ArrayList<>();
        this.nodes = 0;
    }

    // @viewmethod
    @SuppressWarnings({"pico:method.invocation.invalid", "pico:illegal.field.write"})
    public int insert(@Readonly EvilBinarySearchTree this, int value) {
        if (this.root == -1) {
            int newNode = this.nodes;
            this.root = 0;
            while (lefts.size() <= newNode) lefts.add(-1);
            while (rights.size() <= newNode) rights.add(-1);
            while (values.size() <= newNode) values.add(0);

            lefts.set(newNode, -1);
            rights.set(newNode, -1);
            values.set(newNode, value);
            this.nodes += 1;
            return newNode;
        } else {
            int cur = this.root;
            while (true) {
                if (value < this.values.get(cur)) {
                    if (this.lefts.get(cur) == -1) {
                        int newNode = this.nodes;
                        lefts.set(cur, newNode);

                        while (lefts.size() <= newNode) lefts.add(-1);
                        while (rights.size() <= newNode) rights.add(-1);
                        while (values.size() <= newNode) values.add(0);

                        lefts.set(newNode, -1);
                        rights.set(newNode, -1);
                        values.set(newNode, value);
                        this.nodes += 1;
                        return newNode;
                    } else {
                        cur = this.lefts.get(cur);
                    }
                } else {
                    if (this.rights.get(cur) == -1) {
                        int newNode = this.nodes;
                        rights.set(cur, newNode);

                        while (lefts.size() <= newNode) lefts.add(-1);
                        while (rights.size() <= newNode) rights.add(-1);
                        while (values.size() <= newNode) values.add(0);

                        lefts.set(newNode, -1);
                        rights.set(newNode, -1);
                        values.set(newNode, value);
                        this.nodes += 1;
                        return newNode;
                    } else {
                        cur = this.rights.get(cur);
                    }
                }
            }
        }
    }

    // @viewmethod
    public boolean find(@Readonly EvilBinarySearchTree this, int value) {
        int cur = this.root;
        while (cur != -1) {
            if (value < this.values.get(cur)) {
                // :: error: (method.invocation.invalid)
                this.rights.set(cur, 0); // evil!
                cur = this.lefts.get(cur);
            } else if (value > this.values.get(cur)) {
                cur = this.rights.get(cur);
            } else {
                return true;
            }
        }
        return false;
    }
}
