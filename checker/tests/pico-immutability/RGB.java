import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

// Inspire by: https://docs.oracle.com/javase/tutorial/essential/concurrency/imstrat.html
// Allow both mutable and immutable instance creation
// Allow having getters and setters, don't need to remove them
// fields don't need to be declared with "final"
// Don't need defensive copy(even though not applicable in this example)
// Aosen: I don't think this test adding much interest aspect of PICO rules, we can remove this if
// desired.
@ReceiverDependentMutable public class RGB {

    // Values must be between 0 and 255.
    private int red;
    private int green;
    private int blue;
    private String name;

    private void check(@UnknownInitialization @Readonly RGB this, int red, int green, int blue) {
        if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
            throw new IllegalArgumentException();
        }
    }

    public RGB(int red, int green, int blue, String name) {
        check(red, green, blue);
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.name = name;
    }

    public void set(@Mutable RGB this, int red, int green, int blue, String name) {
        check(red, green, blue);
        synchronized (this) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.name = name;
        }
    }

    public synchronized int getRGB(@Readonly RGB this) {
        return ((red << 16) | (green << 8) | blue);
    }

    public synchronized String getName(@Readonly RGB this) {
        return name;
    }

    public synchronized void invert(@Mutable RGB this) {
        red = 255 - red;
        green = 255 - green;
        blue = 255 - blue;
        name = "Inverse of " + name;
    }

    public static void main(String[] args) {
        @Immutable RGB immutable = new @Immutable RGB(0, 0, 0, "black");
        // :: error: (method.invocation.invalid)
        immutable.set(1, 1, 1, "what");
        // :: error: (method.invocation.invalid)
        immutable.invert();
        immutable.getName();
        immutable.getRGB();

        @Mutable RGB mutable = new @Mutable RGB(255, 255, 255, "white");
        mutable.set(1, 1, 1, "what");
        mutable.invert();
        mutable.getName();
        mutable.getRGB();
    }
}
