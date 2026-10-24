// Test case for https://github.com/eisop/checker-framework/issues/2114
// An explicitly typed lambda passed to a parameter of type Function<? super T, ...>.  The type of
// the lambda parameter was equated with T, so T was inferred as Object, and inference crashed when
// T also had to be a subtype of String.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public class EisopIssue2114 {
    static <T> T m(T t, Function<? super T, String> f) {
        return t;
    }

    static <T, U> T bi(T t, U u, BiFunction<? super T, ? super U, String> f) {
        return t;
    }

    static <T> T ext(T t, Function<? super T, ? extends T> f) {
        return t;
    }

    void test(@Nullable String n) {
        String s1 = m("x", (Object o) -> "y");
        String s2 = m("x", (String o) -> "y");
        String s3 = m("x", (CharSequence o) -> "y");
        String s4 = m("x", (@Nullable Object o) -> "y");
        String s5 = bi("x", 1, (Object o, Number u) -> "y");
        String s6 = ext("x", (Object o) -> "y");

        m("x", (Object o) -> "y").length();
        m(n, (@Nullable Object o) -> "y");
        // :: error: (dereference.of.nullable)
        m(n, (@Nullable Object o) -> "y").length();
        // :: error: (type.arguments.not.inferred)
        m(n, (Object o) -> "y");
    }
}
