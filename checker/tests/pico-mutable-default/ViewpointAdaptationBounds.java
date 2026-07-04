import org.checkerframework.checker.mutability.qual.Immutable;
import org.checkerframework.checker.mutability.qual.MutabilityLost;
import org.checkerframework.checker.mutability.qual.Mutable;
import org.checkerframework.checker.mutability.qual.Readonly;
import org.checkerframework.checker.mutability.qual.ReceiverDependentMutable;

public class ViewpointAdaptationBounds {
    @ReceiverDependentMutable class Generic<T extends @ReceiverDependentMutable Object> {}

    void compatibleBounds(
            @Mutable Generic<@Mutable Object> mutable,
            @Immutable Generic<@Immutable Object> immutable) {}

    void receiverDependentArgument(
            // :: error: (type.argument.type.incompatible)
            @Mutable Generic<@ReceiverDependentMutable Object> receiverDependent) {}

    // :: error: (type.argument.type.incompatible)
    void readonlyArgument(@Mutable Generic<@Readonly Object> readonly) {}

    // :: error: (type.argument.type.incompatible)
    void lostArgument(@Mutable Generic<@MutabilityLost Object> lost) {}

    // :: error: (type.argument.type.incompatible)
    void incompatibleMutableBound(@Mutable Generic<@Immutable Object> immutable) {}

    // :: error: (type.argument.type.incompatible)
    void incompatibleImmutableBound(@Immutable Generic<@Mutable Object> mutable) {}

    // Use @MutabilityLost so the type argument is within the adapted @MutabilityLost bound. This
    // isolates the diagnostic for @MutabilityLost in the adapted bound.
    void readonlyMainLostOnlyArgument(@Readonly Generic<@MutabilityLost Object> generic) {
        // :: error: (mutability.lost.lhs)
        @Readonly Generic<@MutabilityLost Object> local = generic;
    }

    void readonlyMainMutableArgument(
            // :: error: (type.argument.type.incompatible)
            @Readonly Generic<@Mutable Object> mutable) {}

    void readonlyMainImmutableArgument(
            // :: error: (type.argument.type.incompatible)
            @Readonly Generic<@Immutable Object> immutable) {}

    void readonlyMainReceiverDependentArgument(
            // :: error: (type.argument.type.incompatible)
            @Readonly Generic<@ReceiverDependentMutable Object> receiverDependent) {}

    void readonlyMainReadonlyArgument(
            // :: error: (type.argument.type.incompatible)
            @Readonly Generic<@Readonly Object> readonly) {}

    void readonlyMainLostArgument(@Readonly Generic<@MutabilityLost Object> lost) {}

    @ReceiverDependentMutable class ReadonlyBounded<T extends @Readonly Object> {}

    class DeclaredTypeParameterBound<T extends @Readonly Generic<@MutabilityLost Object>> {}

    <T extends @Readonly Generic<@MutabilityLost Object>> void declaredMethodTypeParameterBound() {}

    void readonlyBound(
            @Mutable ReadonlyBounded<@Mutable Object> mutable,
            @Mutable ReadonlyBounded<@Immutable Object> immutable,
            @Mutable ReadonlyBounded<@ReceiverDependentMutable Object> receiverDependent,
            @Mutable ReadonlyBounded<@Readonly Object> readonly,
            @Mutable ReadonlyBounded<@MutabilityLost Object> lost) {}

    @ReceiverDependentMutable class Methods {
        <T extends @ReceiverDependentMutable Object> void method(
                @ReceiverDependentMutable Methods this, T t) {}

        <T extends @ReceiverDependentMutable Object> void methodWithNoArgs(
                @ReceiverDependentMutable Methods this) {}
    }

    void callMethod(@Readonly Methods methods, @Mutable Object mutable) {
        // The upper bound of T adapts to @MutabilityLost even though no argument compatibility
        // check runs.
        // :: error: (method.invocation.invalid) :: error: (mutability.lost.in.bounds)
        methods.methodWithNoArgs();

        // Use an explicit type argument to avoid testing type argument inference here.
        // :: error: (method.invocation.invalid) :: error: (type.argument.type.incompatible) ::
        // error: (mutability.lost.in.bounds)
        methods.<@Mutable Object>method(mutable);

        // Even when using null, the adapted method type parameter bound is @MutabilityLost.
        // :: error: (method.invocation.invalid) :: error: (mutability.lost.in.bounds)
        methods.method(null);
    }
}
