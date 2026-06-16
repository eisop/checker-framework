import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class BicounterSecond {
    private @Assignable int count1;
    private int count2;

    public BicounterSecond() {
        this.count1 = 0;
        this.count2 = 0;
    }

    // @immutable
    public void increment1(@Readonly BicounterSecond this) {
        this.count1 += 1;
    }

    public void increment2() {
        this.count2 += 1;
    }

    public int getCount1() {
        return this.count1;
    }

    // @viewmethod
    public int getCount2(@Readonly BicounterSecond this) {
        return this.count2;
    }
}
