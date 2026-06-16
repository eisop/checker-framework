import org.checkerframework.checker.pico.qual.Readonly;

import java.util.List;

// Done, still tricky to understand the abstract state

public class WrongfullyAnnotatedCachedList {
    private List<Integer> list;
    private int cachedIdx;
    private int cachedVal;

    // @viewmethod
    public int get(@Readonly WrongfullyAnnotatedCachedList this, int idx) {
        if (idx != this.cachedIdx) {
            // :: error: (illegal.field.write)
            this.cachedIdx = idx;
            // :: error: (illegal.field.write)
            this.cachedVal = this.list.get(idx);
        }
        return this.cachedVal;
    }

    // @viewmethod
    public void set(@Readonly WrongfullyAnnotatedCachedList this, int idx, int val) {
        if (idx == this.cachedIdx) {
            // :: error: (illegal.field.write)
            this.cachedVal = val;
        }
        // :: error: (method.invocation.invalid)
        this.list.set(idx, val);
    }
}
