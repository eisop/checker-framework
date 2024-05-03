class A {
    Object elements;
}

public class Primitive4 {
    boolean foo(A p1, A p2) {
        return p1.elements == p2.elements;
    }
}
