import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class BicounterFirst {
    private int count1;
    private @Assignable int count2;

    public BicounterFirst() {
        this.count1 = 0;
        this.count2 = 0;
    }

    public void increment1() {
        this.count1 += 1;
    }

    // @immutable
    public void increment2(@Readonly BicounterFirst this) {
        this.count2 += 1;
    }

    // @viewmethod
    public int getCount1(@Readonly BicounterFirst this) {
        return this.count1;
    }

    public int getCount2() {
        return this.count2;
    }
}
