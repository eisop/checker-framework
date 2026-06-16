import org.checkerframework.checker.pico.qual.Immutable;

// Done

@Immutable public class EvilPoint extends Point {
    public EvilPoint(int x, int y) {
        super(x, y);
    }

    public void setX(int param) {
        // :: error: (illegal.field.write)
        this.x = param;
    }

    public void setY(int param) {
        // :: error: (illegal.field.write)
        this.y = param;
    }
}
