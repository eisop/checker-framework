import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class CounterWithAccessCount {
    private int count;
    private @Assignable int accessCount;

    public CounterWithAccessCount() {
        this.count = 0;
    }

    // @immutable
    public int getAccessCount(@Readonly CounterWithAccessCount this) {
        return this.accessCount;
    }

    public void increment() {
        this.count += 1;
        this.accessCount += 1;
    }

    // @viewmethod
    public int get(@Readonly CounterWithAccessCount this) {
        this.accessCount += 1;
        return this.count;
    }
}
