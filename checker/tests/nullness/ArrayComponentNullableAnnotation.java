import org.checkerframework.checker.nullness.qual.Nullable;

public class ArrayComponentNullableAnnotation {
    void foo() {
        int[] @Nullable [] o = new int[10] @Nullable [];
    }
}
