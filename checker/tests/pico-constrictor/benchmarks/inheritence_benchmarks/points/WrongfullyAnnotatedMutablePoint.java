import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class WrongfullyAnnotatedMutablePoint {
    private int x;
    private int y;

    public WrongfullyAnnotatedMutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX(@Readonly WrongfullyAnnotatedMutablePoint this) {
        return this.x;
    }

    public int getY(@Readonly WrongfullyAnnotatedMutablePoint this) {
        return this.y;
    }

    // @viewmethod <-- should not be marked const
    public void setX(@Readonly WrongfullyAnnotatedMutablePoint this, int param) {
        // :: error: (illegal.field.write)
        this.x = param;
    }

    public void setY(int param) {
        this.y = param;
    }
}
