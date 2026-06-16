import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Done

public class HashSet extends GenericSet {
    private Map<Integer, List<Integer>> table;
    private List<Integer> keys;

    public HashSet(List<Integer> arr) {
        this.table = new HashMap<>();
        this.keys = new ArrayList<>();
        for (int elem : arr) {
            int h = elem; // In Java, use hashCode() or simplified hash
            if (this.table.containsKey(h)) {
                this.table.get(h).add(elem);
            } else {
                List<Integer> newList = new ArrayList<>();
                newList.add(elem);
                this.table.put(h, newList);
                this.keys.add(h);
            }
        }
    }

    public boolean present(@Readonly HashSet this, int elem) {
        for (int key : this.keys) {
            if (key == elem) {
                List<Integer> entry = this.table.get(key);
                for (int entryElem : entry) {
                    if (entryElem == elem) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
