import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;

@Immutable // inherits from MemoizedRectangle
public class EvilMemoizedRectangle extends MemoizedRectangle {
    public EvilMemoizedRectangle(int h, int w) {
        super(h, w);
    }

    // @viewmethod
    public int internalSize(@Readonly EvilMemoizedRectangle this) {
        return this.memoSize;
    }
}
