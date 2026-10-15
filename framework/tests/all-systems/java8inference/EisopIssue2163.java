// Test case for https://github.com/eisop/checker-framework/issues/2163
// An unbound method reference to a method inherited from a superclass, such as Sub::name where
// name() is declared in Base, must keep Sub as the receiver type. Using Base made inference of the
// chained call thenComparing crash with "False bound for: Constraint: Base <: Sub".

import java.util.Comparator;

class EisopIssue2163 {
    static class Base {
        String name() {
            return "";
        }
    }

    static class Sub extends Base {
        String other() {
            return "";
        }
    }

    Comparator<Sub> c1 = Comparator.comparing(Sub::name).thenComparing(Sub::other);
    Comparator<Sub> c2 = Comparator.comparing(Sub::name).thenComparing(s -> s.other());
}
