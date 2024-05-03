import org.checkerframework.checker.immutability.qual.*;

public class UnaryAndCompoundAssignment {

    int counter = 0;

    public static void main(String[] args) {
        UnaryAndCompoundAssignment t = new UnaryAndCompoundAssignment();
        t.next();
    }

    public void next(@Readonly UnaryAndCompoundAssignment this) {
        int lcouter = 0;
        lcouter++;
        // :: error: (illegal.field.write)
        counter++;
        lcouter += 5;
        // :: error: (illegal.field.write)
        counter += 5;
    }
}
