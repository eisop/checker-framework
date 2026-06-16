import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class Collatz {
    private @Assignable boolean flag;
    private int someNum;

    // @immutable
    public boolean setFlag(@Readonly Collatz this) {
        this.flag = true;
        return this.flag;
    }

    // @viewmethod
    public int getValue(@Readonly Collatz this) {
        if (!this.flag) {
            return 1;
        } else {
            int ret = this.someNum;
            while (ret != 1) {
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
