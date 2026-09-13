import org.checkerframework.checker.mutability.qual.Immutable;

@Immutable public class GoodPoint extends Point {
    private int z;

    public GoodPoint(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }

    // @viewmethod
    public int getZ() {
        return this.z;
    }
}
