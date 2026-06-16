import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class StringShuffler {
    private String str;

    public StringShuffler(String s) {
        this.str = s;
    }

    // @viewmethod
    public String getString(@Readonly StringShuffler this) {
        return this.str;
    }

    // @immutable
    public void shuffle(@Readonly StringShuffler this) {
        if (this.str.length() != 2) {
            return;
        }
        // :: error: (illegal.field.write)
        this.str = this.str.charAt(1) + String.valueOf(this.str.charAt(0));
        // :: error: (illegal.field.write)
        this.str = this.str.charAt(1) + String.valueOf(this.str.charAt(0));
    }
}
