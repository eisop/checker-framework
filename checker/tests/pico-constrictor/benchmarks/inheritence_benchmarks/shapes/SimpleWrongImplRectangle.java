import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class SimpleWrongImplRectangle {
    private int height;
    private int width;

    public SimpleWrongImplRectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    // @viewmethod <-- should be marked const, but the implementation is incorrect!
    public int getWidth(@Readonly SimpleWrongImplRectangle this) {
        int ret = this.width;
        // :: error: (illegal.field.write)
        this.width = 0;
        return ret;
    }

    // @viewmethod
    public int getHeight(@Readonly SimpleWrongImplRectangle this) {
        return this.height;
    }
}
