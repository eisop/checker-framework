import org.checkerframework.checker.immutability.qual.Immutable;

import java.util.ArrayList;
import java.util.List;

@Immutable
class A {
    int i;

    @Immutable
    A(int i) {
        this.i = i;
    }
}

public class ImmutableListImmutableElement {
    public static void main(String[] args) {
        List<A> l1 = new ArrayList<A>();
        l1.add(new A(0));
        // Wraps in immutable list with the same objects as l1.
        // Cannot add/remove elements from immutable list.
        // Modifying stored element itself is another question
        List<A> l2 = new @Immutable ArrayList<A>(l1);
        // :: error: (method.invocation.invalid)
        l2.add(new A(1));
        // :: error: (illegal.field.write)
        l2.get(0).i = 2;
    }
}
