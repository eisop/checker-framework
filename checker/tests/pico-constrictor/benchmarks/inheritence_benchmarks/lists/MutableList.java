import org.checkerframework.checker.pico.qual.Immutable;

import java.util.ArrayList;

// Done

@Immutable public class MutableList extends ListImpl {
    public MutableList() {
        super(new @Immutable ArrayList<>());
    }

    public void add(int param) {
        // :: error: (method.invocation.invalid)
        this.arr.add(param);
        this.size += 1;
    }
}
