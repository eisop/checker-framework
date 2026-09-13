import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

/**
 * This test checks that PICO issues error for implicit mutable fields in @Immutable and @RDM class.
 */
public class ImplicitMutable {
    @Mutable static class MutableClass {}

    @Immutable static class ExplicitImmutableClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;
        static MutableClass implicitStaticMutableField;

        ExplicitImmutableClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }

    @Immutable static class ImplicitImmutableClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;

        ImplicitImmutableClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }

    @ReceiverDependentMutable static class RDMClass {
        // :: error: (implicit.shallow.immutable)
        MutableClass implicitMutableField;
        @Mutable MutableClass explicitMutableField;

        RDMClass() {
            implicitMutableField = new MutableClass();
            explicitMutableField = new MutableClass();
        }
    }
}
