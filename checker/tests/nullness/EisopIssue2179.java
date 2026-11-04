// Test case for https://github.com/eisop/checker-framework/issues/2179 :
// the body of a lambda returned by a lambda whose target is a raw functional interface type is
// still checked. As for a lambda whose target is written as a raw type, a returned null is
// accepted.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class EisopIssue2179 {
    interface S2<T extends Supplier<List<String>>> {
        T get();
    }

    void test() {
        Supplier raw = () -> null;
        S2 nested = () -> () -> null;
        S2 str = () -> () -> "x";
        S2 body =
                () ->
                        () -> {
                            @Nullable Object o = null;
                            // :: error: (dereference.of.nullable)
                            o.toString();
                            return null;
                        };
        Supplier rawBody =
                () -> {
                    @Nullable Object o = null;
                    // :: error: (dereference.of.nullable)
                    o.toString();
                    return null;
                };
    }
}
