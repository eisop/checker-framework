import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

/**
 * This test checks that PICO issues error for implicit mutable fields in @Immutable and @RDM class.
 */
public class ImplicitMutable {
    @Mutable class MutableClass {}

    @Immutable class ExplicitImmutableClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;

        ExplicitImmutableClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }

    class ImplicitImmutableClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;

        ImplicitImmutableClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }

    @ReceiverDependentMutable class RDMClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;

        RDMClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }
}
