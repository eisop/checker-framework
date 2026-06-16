import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Readonly;

@Immutable // inherits from SizedShape
public class MemoizedRectangle extends SizedShape {
    protected int h;
    protected int w;
    protected @Assignable int memoSize;

    public MemoizedRectangle(int h, int w) {
        this.h = h;
        this.w = w;
        this.memoSize = -1;
    }

    // TODO: override error
    public int size() {
        if (this.memoSize == -1) {
            this.memoSize = this.h * this.w;
        }
        return this.memoSize;
    }

    // @viewmethod
    public int getHeight(@Readonly MemoizedRectangle this) {
        return this.h;
    }

    // @viewmethod
    public int getWidth(@Readonly MemoizedRectangle this) {
        return this.w;
    }
}
