import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Readonly;

@Immutable public class Point {
    protected int x;
    protected int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX(@Readonly Point this) {
        return this.x;
    }

    public int getY(@Readonly Point this) {
        return this.y;
    }
}
