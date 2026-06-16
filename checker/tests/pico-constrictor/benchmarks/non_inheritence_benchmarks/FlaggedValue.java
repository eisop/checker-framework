import org.checkerframework.checker.pico.qual.Readonly;

// Done
public class FlaggedValue {
    private int value;
    private boolean flag;

    public void setFlag() {
        this.flag = true;
    }

    // @viewmethod
    public int getValue(@Readonly FlaggedValue this) {
        if (this.flag) {
            return this.value;
        }
        return 0;
    }
}
