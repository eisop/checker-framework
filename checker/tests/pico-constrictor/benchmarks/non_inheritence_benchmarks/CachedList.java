import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

import java.util.List;

// Done

public class CachedList {
    private List<Integer> list;
    private @Assignable int cachedIdx;
    private @Assignable int cachedVal;

    // @viewmethod
    public int get(@Readonly CachedList this, int idx) {
        if (idx != this.cachedIdx) {
            this.cachedIdx = idx;
            this.cachedVal = this.list.get(idx);
        }
        return this.cachedVal;
    }

    public void set(int idx, int val) {
        if (idx == this.cachedIdx) {
            this.cachedVal = val;
        }
        this.list.set(idx, val);
    }
}
