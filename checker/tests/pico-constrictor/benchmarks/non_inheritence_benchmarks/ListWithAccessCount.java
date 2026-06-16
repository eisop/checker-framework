import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.List;

// Done

public class ListWithAccessCount {
    private @Assignable int accessCount;
    private List<Integer> lst;
    private int size;

    public ListWithAccessCount(List<Integer> lst) {
        this.lst = lst;
        this.size = lst.size();
        this.accessCount = 0;
    }

    // @viewmethod
    public int get(@Readonly ListWithAccessCount this, int param) {
        this.accessCount += 1;
        return this.lst.get(param);
    }

    public void add(int param) {
        this.lst.add(param);
        this.size += 1;
        this.accessCount += 1;
    }

    public void removeLast() {
        this.size -= 1;
        this.accessCount += 1;
    }

    public int getSize() {
        return this.size;
    }
}
