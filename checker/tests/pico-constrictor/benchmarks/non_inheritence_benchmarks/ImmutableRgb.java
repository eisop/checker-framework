// Adapted from https://docs.oracle.com/javase/tutorial/essential/concurrency/imstrat.html
import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class ImmutableRgb {
    private int r;
    private int g;
    private int b;

    public ImmutableRgb(int r, int g, int b) {
        check();
        this.r = r;
        this.g = g;
        this.b = b;
    }

    // @immutable
    private void check(@Readonly ImmutableRgb this) {
        assert (0 <= this.r && this.r <= 255 && 0 <= this.g && this.g <= 255)
                && (0 <= this.b && this.b <= 255);
    }

    public int getRgb(@Readonly ImmutableRgb this) {
        return (this.r * (1 << 16)) + (this.g * (1 << 8)) + this.b;
    }

    public void invert(@Readonly ImmutableRgb this) { // should not be marked const
        // :: error: (illegal.field.write)
        this.r = 255 - this.r;
        // :: error: (illegal.field.write)
        this.g = 255 - this.g;
        // :: error: (illegal.field.write)
        this.b = 255 - this.b;
    }
}
