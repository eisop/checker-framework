import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;

import java.util.List;

// Done

@Immutable public class ListImpl {
    protected List<Integer> arr;
    protected @Assignable int size;

    public ListImpl(@Immutable List<Integer> initial) {
        this.arr = initial;
        this.size = initial.size();
    }

    // @viewmethod
    public int get(int param) {
        return this.arr.get(param);
    }
}
