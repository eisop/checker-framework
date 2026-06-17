// Test case for EISOP issue #1801:
// https://github.com/eisop/checker-framework/issues/1801
import viewpointtest.quals.*;

public class ViewpointAdaptationSuperclassInstantiation {
    static class Super<T> {}

    static class Sub extends Super<@ReceiverDependentQual Object> {}

    void concreteReceivers(@A Sub a, @B Sub b) {
        @A Super<@A Object> aSuper = a;
        @B Super<@B Object> bSuper = b;
        // :: error: (assignment.type.incompatible)
        @B Super<@B Object> badBSuper = a;
        // :: error: (assignment.type.incompatible)
        @A Super<@A Object> badASuper = b;
    }

    void topReceiver(@Top Sub top) {
        // Class type arguments may contain @Lost, so this declaration initialization is valid.
        // @Top viewpoint-adapts @ReceiverDependentQual to @Lost in the type argument.
        @Top Super<@Lost Object> lostSuper = top;
        // Updates are still rejected if the LHS type contains @Lost.
        // :: error: (viewpointtest.lost.lhs)
        lostSuper = top;
        // :: error: (assignment.type.incompatible)
        @Top Super<@A Object> badASuper = top;
        // :: error: (assignment.type.incompatible)
        @Top Super<@B Object> badBSuper = top;
        // :: error: (assignment.type.incompatible)
        @Top Super<@Top Object> badTopSuper = top;
    }
}
