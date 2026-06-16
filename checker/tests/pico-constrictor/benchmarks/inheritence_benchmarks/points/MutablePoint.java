import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class MutablePoint {
    private int x;
    private int y;

    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX(@Readonly MutablePoint this) {
        return this.x;
    }

    public int getY(@Readonly MutablePoint this) {
        return this.y;
    }

    public void setX(int param) {
        this.x = param;
    }

    public void setY(int param) {
        this.y = param;
    }
}
