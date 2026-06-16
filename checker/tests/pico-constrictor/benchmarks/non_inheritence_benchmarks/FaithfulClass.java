import org.checkerframework.checker.pico.qual.Readonly;

public class FaithfulClass {
    private int value;
    private boolean flag;

    public void setFlag() {
        this.flag = true;
    }

    // @viewmethod
    public int getValue(@Readonly FaithfulClass this) {
        if (this.flag) {
            return 0;
        }
        return this.value;
    }
}
