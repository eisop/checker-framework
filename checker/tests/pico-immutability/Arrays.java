import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class Arrays {
    Object[] o = new String[] {""};

    // TODO static array

    void test1(String @Immutable [] array) {
        // :: error: (illegal.array.write)
        array[0] = "something";
    }

    void test2() {
        // :: error: (array.new.invalid)
        int[] a = new int @Readonly [] {1, 2};
    }

    void test3(String[] array) {
        // :: error: (illegal.array.write)
        array[0] = "something";
    }

    void test4(@Immutable String @Mutable [] p) {
        Object[] l = p; // By default, array type is @Readonly(local variable); Object class is by
        // default @Mutable. So assignment should not typecheck
    }

    void test5(@Immutable Integer @Mutable [] p) {
        // :: error: (assignment.type.incompatible)
        @Mutable Object @Readonly [] l = p;
    }

    void test6(double @Readonly [] a1, double @Readonly [] a2) {
        java.util.Arrays.equals(a1, a2);
    }

    void test7() {
        @Readonly Object[] f = new String @Immutable [] {"HELLO"};
    }

    public double @ReceiverDependentMutable [] @Mutable [] test() {
        double @ReceiverDependentMutable [] @Mutable [] C =
                new double @ReceiverDependentMutable [0] @Mutable [0];
        for (@Immutable int i = 0; i < 0; i++) {
            for (@Immutable int j = 0; j < 0; j++) {
                // Array C's main modifier is @ReceiverDependentMutable, so mutating C is not
                // allowed
                // :: error: (illegal.array.write)
                C[i] = new double @Mutable [] {1.0};
                // But C[i] is double @Mutable [](mutable array of double elements), so mutating
                // C[i] is ALLOWED
                C[i][j] = 1.0;
            }
        }
        return C;
    }
}
