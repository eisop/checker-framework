import org.checkerframework.checker.pico.qual.Immutable;

// Done

@Immutable // inherits from SizedShape
public class EvilSquare extends SizedShape {
    private int length;

    public EvilSquare(int length) {
        this.length = length;
    }

    public void setLength(int length) {
        // :: error: (illegal.field.write)
        this.length = length;
    }

    public int size() {
        return this.length * this.length;
    }
}
