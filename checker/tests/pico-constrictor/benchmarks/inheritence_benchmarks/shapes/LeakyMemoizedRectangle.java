import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Readonly;

// Done

@Immutable // inherits from MemoizedRectangle
public class LeakyMemoizedRectangle extends MemoizedRectangle {
    public LeakyMemoizedRectangle(int h, int w) {
        super(h, w);
    }

    // @viewmethod
    public boolean isComputed(@Readonly LeakyMemoizedRectangle this) {
        return this.memoSize != -1;
    }

    // This benchmark is not a violation of view fidelity, but it is a design violation:
    // the existence of isComputed means that now size is not non-mutating, despite us having marked
    // it as such,
    // and despite not overriding it here.
}
