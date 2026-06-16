import org.checkerframework.checker.pico.qual.Immutable;

// Done

@Immutable public class InauspiciousPoint extends Point {
    private int z;

    public InauspiciousPoint(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }

    // @viewmethod
    public int getZ() {
        return this.z;
    }

    public void setZ(int param) {
        // :: error: (illegal.field.write)
        this.z = param;
    }
}
