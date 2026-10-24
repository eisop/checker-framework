// A test that multiple @RequiresNonEmpty annotations can be written on the same
// method and work correctly.

import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.RequiresNonEmpty;

import java.util.ArrayList;

class RepeatedRequiresNonEmpty {
    ArrayList<String> list1 = new ArrayList<>();
    ArrayList<String> list2 = new ArrayList<>();

    @RequiresNonEmpty("this.list1")
    @RequiresNonEmpty("this.list2")
    void test() {}

    void use1() {
        // :: error: (contracts.precondition.not.satisfied)
        test();
    }

    void use2(@NonEmpty ArrayList<String> l1) {
        this.list1 = l1;
        // :: error: (contracts.precondition.not.satisfied)
        test();
    }

    void use3(@NonEmpty ArrayList<String> l2) {
        this.list2 = l2;
        // :: error: (contracts.precondition.not.satisfied)
        test();
    }

    void use4(@NonEmpty ArrayList<String> l1, @NonEmpty ArrayList<String> l2) {
        this.list1 = l1;
        this.list2 = l2;
        test();
    }
}
