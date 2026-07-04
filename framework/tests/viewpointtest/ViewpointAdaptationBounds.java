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

    void topMainBottomArgument(@Top Generic<@Bottom Object> generic) {
        @Top Generic<@Bottom Object> local = generic;
    }

    void topMainAArgument(
            // :: error: (type.argument.type.incompatible)
            @Top Generic<@A Object> a) {}

    void topMainBArgument(
            // :: error: (type.argument.type.incompatible)
            @Top Generic<@B Object> b) {}

    void topMainReceiverDependentArgument(
            // :: error: (type.argument.type.incompatible)
            @Top Generic<@ReceiverDependentQual Object> rdq) {}

    void topMainTopArgument(
            // :: error: (type.argument.type.incompatible)
            @Top Generic<@Top Object> top) {}

    void topMainLostArgument(@Top Generic<@Lost Object> lost) {}

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
        // The upper bound of T adapts to @Lost even though no argument compatibility check runs.
        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        m.methodWithNoArgs();

        // Use an explicit type argument to avoid testing type argument inference here.
        // :: error: (type.argument.type.incompatible) :: error: (viewpointtest.lost.in.bounds)
        m.<@A Object>method(a);

        // Even when using @Bottom, the adapted method type parameter bound is @Lost.
        // :: error: (viewpointtest.lost.in.bounds)
        m.method(b);
    }
}
