import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.List;

// Done
public class MoveToFrontListSet extends GenericSet {
    private List<Integer> list;
    private int size;

    public MoveToFrontListSet(List<Integer> someList) {
        this.list = new ArrayList<>(someList);
        this.size = someList.size();
    }

    // @immutable
    private @Readonly List<Integer> advance(@Readonly MoveToFrontListSet this, int idx) {
        List<Integer> newList = new ArrayList<>();
        newList.add(this.list.get(idx));
        int idx2 = 0;
        while (idx2 < this.size) {
            if (idx != idx2) {
                newList.add(this.list.get(idx2));
            }
            idx2 += 1;
        }
        // :: error: (illegal.field.write) :: error: (assignment.type.incompatible)
        this.list = newList;
        return this.list;
    }

    // @viewmethod
    public boolean present(@Readonly MoveToFrontListSet this, int elem) {
        for (int idx = 0; idx < this.size; idx++) {
            if (this.list.get(idx) == elem) {
                advance(idx);
                return true;
            }
        }
        return false;
    }
}
