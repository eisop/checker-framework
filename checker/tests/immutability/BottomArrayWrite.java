import java.util.Arrays;

public class BottomArrayWrite {
    double[] a = null;

    BottomArrayWrite(double[] a) {
        this.a = a;
    }

    void foo() {
        // If we don't use flow sensitive refinement when a is on lhs,
        // we don't get illegal.array.write via @FBCBottom @Bottom receiver
        // error; But this also fails local variables that can be mutated
        // (refined to/assigned mutable objects).
        a[0] = 1.0;
    }

    void bar() {
        a = new double[1];
    }

    @Override
    public int hashCode() {
        int hash = 0;
        // :: error: (argument.type.incompatible)
        hash = 83 * hash + Arrays.hashCode(a);
        return hash;
    }
}

class Tester {

    public static void foo(BottomArrayWrite b) {
        b.a[0] = 1.0;
    }
}
