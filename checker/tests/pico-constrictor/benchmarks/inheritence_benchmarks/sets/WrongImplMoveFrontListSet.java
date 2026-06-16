import org.checkerframework.checker.pico.qual.Readonly;

import java.util.ArrayList;
import java.util.List;

// Done
public class WrongImplMoveFrontListSet extends GenericSet {
    private List<Integer> list;
    private int size;

    public WrongImplMoveFrontListSet(List<Integer> someList) {
        this.list = new ArrayList<>(someList);
        this.size = someList.size();
    }

    // @immutable
    private void advance(@Readonly WrongImplMoveFrontListSet this, int idx) {
        List<Integer> newList = new ArrayList<>();
        newList.add(this.list.get(idx));
        int currSize = 1;
        idx = 0;
        while (idx < this.size) {
            if (idx != idx) { // This condition is always false - evil!
                while (newList.size() <= currSize) {
                    newList.add(0);
                }
                newList.set(currSize, this.list.get(idx));
                currSize += 1;
            }
            idx += 1;
        }
        // :: error: (illegal.field.write) :: error: (assignment.type.incompatible)
        this.list = new ArrayList<>(); // evil!
    }

    public boolean present(@Readonly WrongImplMoveFrontListSet this, int elem) {
        int idx = 0;
        while (idx < this.size) {
            if (this.list.get(idx) == elem) {
                advance(idx);
                return true;
            }
            idx += 1;
        }
        return false;
    }
}
