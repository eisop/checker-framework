import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;

@Immutable
public class PolyMutableOnConstructorParameters<T> {
    @Immutable
    PolyMutableOnConstructorParameters(@PolyMutable Object o) {}

    public static void main(String[] args) {
        @Immutable
        PolyMutableOnConstructorParameters o1 =
                new @Immutable PolyMutableOnConstructorParameters(new @Immutable Object());
    }
}
