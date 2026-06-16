import org.checkerframework.checker.pico.qual.Immutable;

@Immutable public class StaticAssignment {
    public static int GLOBAL = 1; // OK, no error here

    StaticAssignment() {
        // ::error: (glacier.assignment)
        GLOBAL = 2;
    }

    public void setStatics() {
        // ::error: (glacier.assignment)
        GLOBAL = 42;
    }
}
