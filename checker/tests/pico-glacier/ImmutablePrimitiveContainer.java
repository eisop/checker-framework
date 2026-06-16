import org.checkerframework.checker.pico.qual.Immutable;

@Immutable public class ImmutablePrimitiveContainer {
    int x;

    public void setX(int x) {
        // ::error: (glacier.assignment)
        this.x = x;
    }
}
