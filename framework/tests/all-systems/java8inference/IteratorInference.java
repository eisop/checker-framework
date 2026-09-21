import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

@SuppressWarnings({"unchecked", "all"})
public class IteratorInference {
    static <T extends @Nullable Object> void concatNoDefensiveCopy(
            Iterator<? extends T>... inputs) {
        for (Iterator<? extends T> input : checkNotNull(inputs)) {}
    }

    public static <T> T checkNotNull(T reference) {
        return reference;
    }
}
