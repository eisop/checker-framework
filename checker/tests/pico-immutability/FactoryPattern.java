import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.PolyMutable;
import org.checkerframework.checker.pico.qual.Readonly;

public class FactoryPattern {
    public @Immutable FactoryPattern() {}

    @PolyMutable Object createObject(@Readonly FactoryPattern this) {
        return new @PolyMutable Object();
    }

    static void test() {
        @Immutable FactoryPattern factory = new @Immutable FactoryPattern();
        // Both typecheck in new PICO
        @Mutable Object mo = factory.createObject();
        @Immutable Object imo = factory.createObject();
    }
}
