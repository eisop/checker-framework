import org.checkerframework.checker.pico.qual.Readonly;

// Done
public class WrongfullyImplementedRectangle extends ColoredShape {
    private int height;
    private int width;

    public WrongfullyImplementedRectangle(int color, int width, int height) {
        super(color);
        this.width = width;
        this.height = height;
    }

    // @viewmethod <-- should be marked const, but the implementation is incorrect!
    public int getWidth(@Readonly WrongfullyImplementedRectangle this) {
        int ret = this.width;
        // :: error: (illegal.field.write)
        this.width = 0;
        return ret;
    }

    // @viewmethod
    public int getHeight(@Readonly WrongfullyImplementedRectangle this) {
        return this.height;
    }

    public void setWidth(int param) {
        this.width = param;
    }

    public void setHeight(int param) {
        this.height = param;
    }

    // @viewmethod
    public int getArea(@Readonly WrongfullyImplementedRectangle this) {
        return this.width * this.height;
    }
}
