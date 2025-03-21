// Test case for Issue1111
// https://github.com/typetools/checker-framework/issues/1111
// Additional test case in framework/tests/all-systems/Issue1111.java

import org.checkerframework.checker.tainting.qual.Untainted;

import java.util.List;

public class Issue1111 {
    void foo(Box<? super Integer> box, List<Integer> list) {
        // :: error: (type.arguments.not.inferred)
        bar(box, list);
    }

    void foo2(Box<@Untainted ? super Integer> box, List<Integer> list) {
        // :: error: (type.arguments.not.inferred)
        bar(box, list);
    }

    void foo3(Box<@Untainted ? super Integer> box, List<@Untainted Integer> list) {
        bar(box, list);
    }

    <T extends Number> void bar(Box<T> box, Iterable<? extends T> list) {}

    class Box<T extends Number> {}
}
