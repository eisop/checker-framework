import org.checkerframework.checker.nullness.qual.Nullable;

public class ArrayMainModifierNullableAnnotation {
    void foo() {
        // :: warning: (new.array.nullable.ignored)
        int[] o = new int @Nullable [10];
    }
}
