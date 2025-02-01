import org.checkerframework.checker.pico.qual.Immutable;
import org.checkerframework.checker.pico.qual.Mutable;
import org.checkerframework.checker.pico.qual.Readonly;
import org.checkerframework.checker.pico.qual.ReceiverDependentMutable;

public class Arrays {
    Object[] o = new String[] {""};
    Object[] o1 = new Object[10];
    Object[] o2 = new @ReceiverDependentMutable Object[10];
    // :: error: (assignment.type.incompatible)
    @Mutable Object[] o3 = new Object[10];
    // :: error: (assignment.type.incompatible) rhs is resolved to @Immutable Object @Immutable []
    // based on the class bound @Immutable
    @Mutable Object[] o4 = new @ReceiverDependentMutable Object[10];
    static Object[] o5 = new Object[10];
    static Object[] o6 = new String[10];
    static Object[] o7 = new @ReceiverDependentMutable Object[10];
    // :: error: (assignment.type.incompatible)
    static @Mutable Object[] o8 = new Object[10];
    // :: error: (assignment.type.incompatible)
    static @Mutable Object[] o9 = new @ReceiverDependentMutable Object[10];

    @Mutable class MutableClass {
        int[] a; // RDM component array resolve as mutable

        void test() {
            a[0] = 1;
        }
    }

    class ImmutableClass {
        // :: error: (initialization.field.uninitialized)
        int[] a; // RDM component array resolve as Immutable

        void test() {
            // :: error: (illegal.array.write)
            a[0] = 1;
        }
    }

    @ReceiverDependentMutable class RDMClass {
        // :: error: (initialization.field.uninitialized)
        int[] a;

        void testRDMReceiver(@ReceiverDependentMutable RDMClass this) {
            // :: error: (illegal.array.write)
            a[0] = 1; // RDM component array resolve as RDM, can not write to RDM
        }

        void testImmutableReceiver(@Immutable RDMClass this) {
            // :: error: (illegal.array.write)
            a[0] = 1; // RDM component array resolve as immutable
        }

        void testMutableReceiver(@Mutable RDMClass this) {
            a[0] = 1; // RDM component array resolve as mutable
        }
    }

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
