import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.PolyMutable;
import org.checkerframework.checker.immutability.qual.Readonly;

@Immutable
public class ImmutabilityFactoryPattern {
    public @Immutable ImmutabilityFactoryPattern() {}

    @PolyMutable
    Object createObject(@Readonly ImmutabilityFactoryPattern this) {
        return new @PolyMutable Object();
    }

    static void test() {
        @Immutable ImmutabilityFactoryPattern factory = new @Immutable ImmutabilityFactoryPattern();
        // Both typecheck in new PICO
        @Mutable Object mo = factory.createObject();
        @Immutable Object imo = factory.createObject();
    }
}
