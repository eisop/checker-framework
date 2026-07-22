import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.PolyMutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

@Immutable public class MutabilityTypeUseLocation {
    // :: error: (type.invalid.conflicting.annos)
    @Readonly @Immutable Object field;
    // :: error: (type.invalid.conflicting.annos)
    String @Readonly @Immutable [] array;
    // In the abstract state
    int implicitImmutableInt;
    @Immutable int validInt;
    // If you want to exclude primitive(including boxed primitive) and String from
    // abstract state, use @Readonly to do this, but not @Mutable, because they can't
    // be mutated conceptually.
    // :: error: (type.invalid.annotations.on.use)
    @Readonly int implicitOverridenInt;
    // :: error: (type.invalid.annotations.on.use)
    @Mutable int invalidInt;
    // :: error: (type.invalid.annotations.on.use)
    @ReceiverDependentMutable int invalidInt2;

    // :: error: (initialization.fields.uninitialized)
    MutabilityTypeUseLocation() {}

    // :: error: (type.invalid.annotations.on.location) :: error: (invalid.polymorphic.qualifier)
    class MutabilityTypeUseLocationFail<@PolyMutable T, S extends @PolyMutable Object> {
        // :: error: (type.invalid.annotations.on.location) :: error: (constructor.return.invalid)
        @PolyMutable MutabilityTypeUseLocationFail() {}

        // :: error: (super.invocation.invalid) :: error: (constructor.return.invalid)
        @Readonly MutabilityTypeUseLocationFail(int a) {}
    }
}
