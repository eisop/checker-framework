import org.checkerframework.checker.pico.qual.Assignable;
import org.checkerframework.checker.pico.qual.Immutable;

// Done

@Immutable public class AlrightPoint extends Point {
    private @Assignable int distance;

    public AlrightPoint(int x, int y) {
        super(x, y);
        this.distance = -1;
    }

    public int distanceSq() {
        if (this.distance == -1) {
            this.distance = this.x * this.x + this.y * this.y;
        }
        return this.distance;
    }
}
