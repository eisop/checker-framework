import org.checkerframework.checker.pico.qual.Readonly;

// Done
public class ColoredShape {
    protected int color;

    public ColoredShape(int color) {
        this.color = color;
    }

    // @viewmethod
    public int getColor(@Readonly ColoredShape this) {
        return this.color;
    }

    public void setColor(int param) {
        this.color = param;
    }

    public int getArea() {
        return -1;
    }
}
