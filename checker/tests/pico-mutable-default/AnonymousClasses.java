import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

class AnonymousClasses {
    @Immutable static class ImmutableClass {}

    @Mutable static class MutableClass {}

    @ReceiverDependentMutable static class RDMClass {}

    void creationExpressionDeterminesAnonymousClassBound() {
        new @Immutable ImmutableClass() {};

        // :: error: (type.invalid.annotations.on.use) :: warning:
        // (cast.unsafe.constructor.invocation)
        new @Mutable ImmutableClass() {};

        new @Mutable MutableClass() {};

        // :: error: (type.invalid.annotations.on.use) :: warning:
        // (cast.unsafe.constructor.invocation)
        new @Immutable MutableClass() {};

        new @Mutable RDMClass() {};
        new @Immutable RDMClass() {};
        new @ReceiverDependentMutable RDMClass() {};

        // :: error: (constructor.invocation.invalid) :: error: (constructor.return.invalid)
        new @Readonly RDMClass() {};
    }
}
