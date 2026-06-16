import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class LongLoopMutator {
    private int x;

    // @viewmethod
    public int getValue(@Readonly LongLoopMutator this) {
        return this.x;
    }

    // @viewmethod
    public void mutate(@Readonly LongLoopMutator this) {
        for (int i = 0; i < 200; i++) {
            // empty loop
        }
        // :: error: (illegal.field.write)
        this.x += 1;
    }
}
