import org.checkerframework.checker.pico.qual.Readonly;

// Done

public class VariableTypesMatter {
    private int someInt;
    private String someStr;

    // @immutable
    public void shuffleStrings(@Readonly VariableTypesMatter this, String s1, String s2) {
        if (this.someStr.equals(s1 + s2)) {
            // :: error: (illegal.field.write)
            this.someStr = s2 + s1;
        }
    }

    // @immutable
    public void shuffleInts(@Readonly VariableTypesMatter this, int i1, int i2) {
        if (this.someInt == i1 + i2) {
            // :: error: (illegal.field.write)
            this.someInt = i2 + i1;
        }
    }

    // @viewmethod
    public int getSomeInt(@Readonly VariableTypesMatter this) {
        return this.someInt;
    }

    // @viewmethod
    public String getSomeStr(@Readonly VariableTypesMatter this) {
        return this.someStr;
    }
}
