import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class WrongfullyImplementedCollatz {
    private boolean flag;
    private int someNum;

    // @immutable
    public void setFlag(@Readonly WrongfullyImplementedCollatz this) {
        // :: error: (illegal.field.write)
        this.flag = true;
    }

    // @viewmethod
    public int getValue(@Readonly WrongfullyImplementedCollatz this) {
        if (!this.flag) {
            return 1;
        } else {
            int ret = this.someNum;
            while (ret != 2) { // evil!
                if (ret % 2 == 0) {
                    ret = ret / 2;
                } else {
                    ret = ret * 3 + 1;
                }
            }
            return ret;
        }
    }
}
