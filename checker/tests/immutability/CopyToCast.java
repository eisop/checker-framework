import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;

public class CopyToCast {
    void foo(Object o) {
        // No cast.unsafe
        String s1 = (@Immutable String) o;
        // No cast.unsafe
        String s2 = (String) o;
        // :: error: (type.invalid.annotations.on.use)
        String s3 = (@Mutable String) o;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        // TODO Copy method receiver's annotation to super
        // TODO Defaults for four Object methods are fixed
        CopyToCast oe = (CopyToCast) super.clone();
        return oe;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
