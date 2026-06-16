import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class Rectangle extends ColoredShape {
    private int height;
    private int width;

    public Rectangle(int color, int width, int height) {
        super(color);
        this.width = width;
        this.height = height;
    }

    // @viewmethod
    public int getWidth(@Readonly Rectangle this) {
        return this.width;
    }

    // @viewmethod
    public int getHeight(@Readonly Rectangle this) {
        return this.height;
    }

    public void setWidth(int param) {
        this.width = param;
    }

    public void setHeight(int param) {
        this.height = param;
    }

    public int getArea() {
        return this.width * this.height;
    }
}
