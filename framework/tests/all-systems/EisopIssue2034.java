// Test case for https://github.com/eisop/checker-framework/issues/2034 .
// Capture conversion of a wildcard whose type parameter is F-bounded.
// The capture of Node<? extends Sub> is Node<CAP#1>, where javac computes the upper bound of CAP#1
// as glb(Sub, Node<CAP#1>) = Sub. The Checker Framework used to compute a different upper bound,
// which led to crashes in the validation of every use of such a type.

import java.util.concurrent.TimeUnit;

public class EisopIssue2034 {
    static class Node<T extends Node<T>> {
        T get() {
            throw new Error();
        }
    }

    static class Sub extends Node<Sub> {}

    static class Mid<M extends Mid<M>> extends Node<M> {}

    static class Sub2 extends Mid<Sub2> {}

    interface Tag<T> {}

    static class MultiNode<T extends MultiNode<T> & Tag<T>> {}

    static class MultiSub extends MultiNode<MultiSub> implements Tag<MultiSub> {}

    static class Pair<A extends Pair<A, B>, B> {}

    static class P extends Pair<P, String> {}

    Node<? extends Sub> field = new Sub();

    static class ClassBound<U extends Node<? extends Sub>> {}

    <U extends Node<? extends Sub>> void methodBound(U u) {}

    void parameter(Node<? extends Sub> n) {}

    void deeper(Node<? extends Sub2> n, Mid<? extends Sub2> m) {}

    void multipleBounds(MultiNode<? extends MultiSub> n) {}

    void twoTypeParameters(Pair<? extends P, String> p, Pair<? extends P, ?> q) {}

    void jdkEnum(Enum<? extends TimeUnit> e) {}

    <E extends Enum<E>> void jdkEnumTypeVar(Enum<? extends E> e) {}

    Sub local(Node<Sub> in) {
        Node<? extends Sub> n = in;
        Sub s = n.get();
        return s.get();
    }
}
