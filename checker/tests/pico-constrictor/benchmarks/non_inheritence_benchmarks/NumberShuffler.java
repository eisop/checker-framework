import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class NumberShuffler {
    private int theNumber;

    // @viewmethod
    public int getTheNumber(@Readonly NumberShuffler this) {
        return this.theNumber;
    }

    // @immutable - mutating
    public void shuffle1(@Readonly NumberShuffler this) {
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber * 2;
    }

    // @immutable - mutating
    public void shuffle2(@Readonly NumberShuffler this) {
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber + 2;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber * 2;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber / 2 - 1;
    }

    // @immutable - non-mutating
    public void shuffle3(@Readonly NumberShuffler this) {
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber * 2;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber + 2;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber / 2 - 1;
    }

    // @immutable - non-mutating
    public void shuffle4(@Readonly NumberShuffler this) {
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber * 2;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber / 2 - 1;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber + 1;
    }

    // @immutable - non-mutating
    public void shuffle5(@Readonly NumberShuffler this, int param) {
        // :: error: (illegal.field.write)
        this.theNumber = 5 * param + 3 * this.theNumber;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber - 5 * this.theNumber;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber + 20 * param;
        // :: error: (illegal.field.write)
        this.theNumber = this.theNumber / -12;
    }
}
