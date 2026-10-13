// Test case for https://github.com/eisop/checker-framework/issues/2160
// A return statement in a method of an anonymous or local class declared in a lambda body returns
// from that method, not from the lambda. It was treated as a result expression of the lambda,
// which crashed type argument inference.

import java.util.List;
import java.util.function.Supplier;

public class EisopIssue2160 {
    static <T> List<T> defer(Supplier<? extends List<? extends T>> s) {
        throw new Error();
    }

    static <T> T get(Supplier<T> s) {
        throw new Error();
    }

    List<String> anonymous(List<String> list) {
        return defer(
                () -> {
                    new Object() {
                        Object f() {
                            return "";
                        }
                    };
                    return list;
                });
    }

    List<String> local(List<String> list) {
        return defer(
                () -> {
                    class Local {
                        Object f() {
                            return "";
                        }
                    }
                    return list;
                });
    }

    List<String> invariant(List<String> list) {
        return get(
                () -> {
                    new Object() {
                        Object f() {
                            return "";
                        }
                    };
                    return list;
                });
    }
}
