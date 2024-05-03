import org.checkerframework.checker.immutability.qual.Immutable;
import org.checkerframework.checker.immutability.qual.Mutable;
import org.checkerframework.checker.immutability.qual.Readonly;

public class Arrays {

    void test1(String @Immutable [] array) {
        // :: error: (illegal.array.write)
        array[0] = "something";
    }

    void test2() {
        // :: error: (pico.new.invalid)
        int[] a = new int @Readonly [] {1, 2};
    }

    void test3(String[] array) {
        array[0] = "something";
    }

    void test4(@Immutable String @Mutable [] p) {
        // :: error: (assignment.type.incompatible)
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
}
