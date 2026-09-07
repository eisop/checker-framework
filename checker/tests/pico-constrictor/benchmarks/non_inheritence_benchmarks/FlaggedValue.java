import org.checkerframework.checker.mutability.qual.Readonly;

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
