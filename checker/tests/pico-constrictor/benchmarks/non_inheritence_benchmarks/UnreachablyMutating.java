import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class UnreachablyMutating {
    private int i;

    public UnreachablyMutating() {
        this.i = 0;
    }

    // @viewmethod
    public int doNotMutate(@Readonly UnreachablyMutating this) {
        if (this.i < 0) { // this state is not reachable!
            // :: error: (illegal.field.write)
            this.i = 0;
        }
        return this.i;
    }

    // @viewmethod
    public int get(@Readonly UnreachablyMutating this) {
        return this.i;
    }
}
