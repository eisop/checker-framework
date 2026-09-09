import viewpointtest.quals.*;

// An F-bounded class used raw has a cyclic type graph: the implicit wildcard bound leads back to
// the same declared type. Viewpoint adaptation must still produce the adapted qualifier at every
// position, rather than leaving the original in place.
@SuppressWarnings("rawtypes")
public class RawFBoundedAdaptation {
    // The bound is receiver-dependent, so the qualifier to adapt lies inside the cycle.
    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual static class Rec<T extends @ReceiverDependentQual Rec<T>> {}

    @SuppressWarnings({"inconsistent.constructor.type", "super.invocation.invalid"})
    @ReceiverDependentQual static class Sub extends Rec<Sub> {}

    // Not F-bounded: used below to vary the primary qualifier and the type argument
    // independently, which is orthogonal to whether the graph is cyclic.
    static class Box<T> {}

    static class Holder {
        // Cyclic: the F-bounded class used raw.
        @ReceiverDependentQual Rec raw;

        // The same class with an explicit type argument, so the graph is finite. Adaptation must
        // give the same qualifiers as the raw case.
        @ReceiverDependentQual Rec<@ReceiverDependentQual Sub> explicitArg;

        // Primary qualifier and type argument, each independently receiver-dependent or fixed.
        @ReceiverDependentQual Box<@ReceiverDependentQual Object> bothAdapt;
        @ReceiverDependentQual Box<@B Object> primaryAdaptsArgFixed;
        @B Box<@ReceiverDependentQual Object> primaryFixedArgAdapts;
    }

    void rawIsAdapted(@A Holder a, @B Holder b) {
        @A Rec fromA = a.raw;
        @B Rec fromB = b.raw;

        // :: error: (assignment.type.incompatible)
        @B Rec wrongFromA = a.raw;
        // :: error: (assignment.type.incompatible)
        @A Rec wrongFromB = b.raw;
    }

    void explicitTypeArgumentIsAdapted(@A Holder a, @B Holder b) {
        @A Rec<@A Sub> fromA = a.explicitArg;
        @B Rec<@B Sub> fromB = b.explicitArg;
    }

    // The type-parameter bound is adapted along with everything else: `T extends @RDQ Rec<T>`
    // becomes `T extends @A Rec<T>` through an @A receiver, so only @A satisfies it there. That
    // bound is reached through the cycle, so these are the cases that pin adaptation of the
    // back edge.
    void adaptedBoundRejectsOtherQualifiers(@A Holder a, @B Holder b) {
        // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
        @A Rec<@B Sub> siblingFromA = a.explicitArg;
        // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
        @B Rec<@A Sub> siblingFromB = b.explicitArg;

        // @Top is outside Sub's declaration bound, so it is rejected at the use as well.
        // :: error: (type.argument.type.incompatible) :: error: (type.invalid.annotations.on.use)
        @A Rec<@Top Sub> topFromA = a.explicitArg;

        // @Bottom satisfies the adapted bound, but is not the adapted type argument.
        // :: error: (assignment.type.incompatible)
        @A Rec<@Bottom Sub> bottomFromA = a.explicitArg;
    }

    void primaryAndTypeArgumentAreIndependent(@A Holder a, @B Holder b) {
        // Both positions are receiver-dependent, so both adapt.
        @A Box<@A Object> bothFromA = a.bothAdapt;
        @B Box<@B Object> bothFromB = b.bothAdapt;
        // :: error: (assignment.type.incompatible)
        @A Box<@B Object> bothWrongFromA = a.bothAdapt;

        // A fixed type argument is left alone while the primary qualifier adapts.
        @A Box<@B Object> argFixedFromA = a.primaryAdaptsArgFixed;
        @B Box<@B Object> argFixedFromB = b.primaryAdaptsArgFixed;
        // :: error: (assignment.type.incompatible)
        @A Box<@A Object> argFixedWrongFromA = a.primaryAdaptsArgFixed;

        // A fixed primary qualifier is left alone while the type argument adapts.
        @B Box<@A Object> primaryFixedFromA = a.primaryFixedArgAdapts;
        @B Box<@B Object> primaryFixedFromB = b.primaryFixedArgAdapts;
        // :: error: (assignment.type.incompatible)
        @A Box<@A Object> primaryFixedWrongFromA = a.primaryFixedArgAdapts;
    }
}
