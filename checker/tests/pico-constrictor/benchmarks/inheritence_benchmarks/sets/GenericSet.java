import org.checkerframework.checker.mutability.qual.Readonly;

public abstract class GenericSet {
    // @viewmethod
    public boolean present(@Readonly GenericSet this, int elem) {
        return false;
    }
}
