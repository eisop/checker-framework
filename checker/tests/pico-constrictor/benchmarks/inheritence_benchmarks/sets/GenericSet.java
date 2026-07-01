import org.checkerframework.checker.pico.qual.Readonly;

public abstract class GenericSet {
    // @viewmethod
    public boolean present(@Readonly GenericSet this, int elem) {
        return false;
    }
}
