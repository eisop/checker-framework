class A {}

public class AnonymousClassBound {
    static A[] as = new A[1];

    static {
        as[0] = new A() {};
    }
}
