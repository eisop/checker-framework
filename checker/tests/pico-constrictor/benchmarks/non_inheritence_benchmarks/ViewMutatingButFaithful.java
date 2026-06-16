import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class ViewMutatingButFaithful {
    private int x;

    // @viewmethod
    public int someViewMethod(@Readonly ViewMutatingButFaithful this) {
        int ret = this.x;
        // :: error: (illegal.field.write)
        this.x = 0;
        return ret;
    }

    // @viewmethod
    public int getX(@Readonly ViewMutatingButFaithful this) {
        return this.x;
    }
}
