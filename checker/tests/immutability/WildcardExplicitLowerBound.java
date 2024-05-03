import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.ReceiverDependantMutable;

import java.util.List;

class A {}

@Immutable
class B {
    @Immutable
    B() {}
}

@ReceiverDependantMutable
class C {
    @ReceiverDependantMutable
    C() {}
}

public class WildcardExplicitLowerBound {

    // Doesn't allow explicit usage of @Bottom on explicit lower bound anymore, as it adds
    // difficulty to validate correct usage of that, because in type validator, dataflow
    // refinement's result might also causes its warning. There is no way of differentiating
    // explicit usage and internal usage right now.
    void test1(List<? super A> l) {}

    void test2(List<? super B> l) {}

    void test3(List<? super C> l) {}
}
