import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Done

public class EvilHashSet extends GenericSet {
    private Map<Integer, List<Integer>> table;
    private Map<Integer, Integer> tableSizes;
    private List<Integer> keys;
    private int numKeys;

    public EvilHashSet(List<Integer> arr) {
        this.table = new HashMap<>();
        this.tableSizes = new HashMap<>();
        this.keys = new ArrayList<>();
        for (int elem : arr) {
            int h = elem; // Simplified hash
            if (this.table.containsKey(h)) {
                this.table.get(h).add(elem);
                this.tableSizes.put(h, this.tableSizes.get(h) + 1);
            } else {
                List<Integer> newList = new ArrayList<>();
                newList.add(elem);
                this.table.put(h, newList);
                this.keys.add(h);
                this.tableSizes.put(h, 1);
            }
        }
        this.numKeys = this.keys.size();
    }

    // @viewmethod
    public boolean present(@Readonly EvilHashSet this, int elem) {
        int idx = 0;
        while (idx < this.numKeys) {
            if (this.keys.get(idx) == elem) {
                List<Integer> entry = this.table.get(this.keys.get(idx));
                int entrySize = this.tableSizes.get(this.keys.get(idx));
                int idx2 = 0;
                while (idx2 < entrySize) {
                    if (entry.get(idx2) == elem) {
                        return true;
                    }
                    idx2 += 1;
                }
            }
            idx += 1;
        }
        // evil!
        Map<Integer, List<Integer>> evilMap = new HashMap<>();
        List<Integer> evilList = new ArrayList<>();
        evilList.add(5);
        evilMap.put(1, evilList);
        // One more evil in PICO because of field write is prevented and the type is not compatible
        // :: error: (illegal.field.write) :: error: (assignment.type.incompatible)
        this.table = evilMap;
        return false;
    }
}
