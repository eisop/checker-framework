class A {
    int size() {
        return 0;
    }
}

public class Primitive2 {
    void foo(A a) {
        double mean1 = mean(a);
    }

    static double mean(A a) {
        return sum(a) / a.size();
    }

    static double sum(A a) {
        return 1.0;
    }
}
