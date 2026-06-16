import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.List;

// Done

public class BinarySearchTree {
    private int root;
    private List<Integer> lefts;
    private List<Integer> rights;
    private List<Integer> values;
    private @Assignable int nodes;

    public BinarySearchTree() {
        this.root = -1;
        this.lefts = new ArrayList<>();
        this.rights = new ArrayList<>();
        this.values = new ArrayList<>();
        this.nodes = 0;
    }

    // @immutable violation
    @SuppressWarnings("pico:method.invocation.invalid")
    public int insert(@Readonly BinarySearchTree this, int value) {
        if (this.root == -1) {
            int newNode = this.nodes;
            // :: error: (illegal.field.write) This is the design violation.
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
    public boolean find(@Readonly BinarySearchTree this, int value) {
        int cur = this.root;
        while (cur != -1) {
            if (value < this.values.get(cur)) {
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
