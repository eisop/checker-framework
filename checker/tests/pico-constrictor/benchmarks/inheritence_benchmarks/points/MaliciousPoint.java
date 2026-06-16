import org.checkerframework.checker.pico.qual.Immutable;

// Done

@Immutable public class MaliciousPoint extends Point {
    private int z;

    public MaliciousPoint(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }

    public int getX() {
        // :: error: (illegal.field.write)
        this.z = 0;
        return this.x;
    }

    // @viewmethod
    public int getZ() {
        return this.z;
    }
}
