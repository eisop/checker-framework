import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;

public class CastTest {
    void foo(Object o) {
        // No cast.unsafe
        String s1 = (@Immutable String) o;
        // No cast.unsafe
        String s2 = (String) o;
        // :: error: (type.invalid.annotations.on.use)
        String s3 = (@Mutable String) o;
    }

    @Override
    // :: error: (override.return.invalid)
    public Object clone() throws CloneNotSupportedException {
        CastTest oe = (CastTest) super.clone();
        return oe;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
