import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;

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
    public Object clone() throws CloneNotSupportedException {
        CastTest oe = (CastTest) super.clone();
        return oe;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
