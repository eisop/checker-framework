import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Issue771 {
    private @MonotonicNonNull String value = null;

    @RequiresNonNull("value")
    void print() {
        System.out.println(value);
    }

    void func() {
        if (value == null) {
            return;
        }

        Runnable r = () -> print();
    }

    private final @Nullable String finalValue;

    Issue771(@Nullable String finalValue) {
        this.finalValue = finalValue;
    }

    void finalField() {
        if (finalValue == null) {
            return;
        }

        Runnable r = () -> finalValue.toString();
    }

    private @Nullable String nullableValue;

    void nullableField() {
        if (nullableValue == null) {
            return;
        }

        Runnable r =
                () -> {
                    // :: error: (dereference.of.nullable)
                    nullableValue.toString();
                };
        nullableValue = null;
        r.run();
    }
}
