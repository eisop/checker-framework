import org.checkerframework.checker.mutability.qual.Assignable;
import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;

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

    // No override error is expected here: size() retains the @Readonly receiver from
    // the superclass, and mutating memoSize is allowed because it is marked @Assignable.
    public int size(@Readonly MemoizedRectangle this) {
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
