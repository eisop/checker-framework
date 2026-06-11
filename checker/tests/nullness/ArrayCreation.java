import org.checkerframework.checker.nullness.qual.Nullable;

public class ArrayCreation {

    void foo() {
        // :: error: (nullness.on.new.array)
        int[] o = new int @Nullable [10];
    }

    void bar() {
        int[] @Nullable [] o = new int[10] @Nullable [];
    }
}
