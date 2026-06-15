import viewpointtest.quals.*;

public class ViewpointAdaptationBounds {
    static class Generic<T extends @ReceiverDependentQual Object> {}

    void compatibleBounds(@A Generic<@A Object> a, @B Generic<@B Object> b) {}

    // :: error: (type.argument.type.incompatible)
    void receiverDependentArgument(@A Generic<@ReceiverDependentQual Object> rdq) {}

    // :: error: (type.argument.type.incompatible)
    void topArgument(@A Generic<@Top Object> top) {}

    // :: error: (type.argument.type.incompatible)
    void lostArgument(@A Generic<@Lost Object> lost) {}

    // :: error: (type.argument.type.incompatible)
    void incompatibleABound(@A Generic<@B Object> b) {}

    // :: error: (type.argument.type.incompatible)
    void incompatibleBBound(@B Generic<@A Object> a) {}

    // Use @Bottom so the type argument is within the adapted @Lost bound. That isolates the
    // diagnostic for @Lost in the adapted bound.
    void topMainBottomArgument(
            // :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@Bottom Object> generic) {
        // :: error: (viewpointtest.lost.in.bounds)
        @Top Generic<@Bottom Object> local = generic;
    }

    void topMainAArgument(
            // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@A Object> a) {}

    void topMainBArgument(
            // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@B Object> b) {}

    void topMainReceiverDependentArgument(
            // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@ReceiverDependentQual Object> rdq) {}

    void topMainTopArgument(
            // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@Top Object> top) {}

    void topMainLostArgument(
            // :: error: (viewpointtest.lost.in.bounds)
            @Top Generic<@Lost Object> lost) {}

    static class TopBounded<T extends @Top Object> {}

    void topBound(
            @A TopBounded<@A Object> a,
            @A TopBounded<@B Object> b,
            @A TopBounded<@ReceiverDependentQual Object> rdq,
            @A TopBounded<@Top Object> top,
            @A TopBounded<@Lost Object> lost) {}

    static class Methods {
        <T extends @ReceiverDependentQual Object> void method(T t) {}

        <T extends @ReceiverDependentQual Object> void methodWithNoArgs() {}
    }

    void callMethod(@Top Methods m, @A Object a, @Bottom Object b) {
        // Here, the upper bound of T adapts to @Lost, but because we pass no arguments,
        // no argument compatibility check fails. If method type parameter bounds were checked,
        // this would report `viewpointtest.lost.in.bounds`. Currently, it reports 0 errors,
        // clearly illustrating the missing bounds check for method invocations!
        m.methodWithNoArgs();

        // When arguments are provided, it fails subtyping because no qualifier
        // is a subtype of @Lost.
        // :: error: (argument.type.incompatible) :: error: (viewpointtest.lost.parameter)
        m.method(a);

        // Even when using @Bottom, the argument is incompatible because @Lost is intentionally
        // excluded from the standard qualifier hierarchy (i.e., @Bottom <: @Lost is false).
        // :: error: (argument.type.incompatible) :: error: (viewpointtest.lost.parameter)
        m.method(b);
    }
}
