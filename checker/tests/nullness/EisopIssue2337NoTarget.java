// @below-java10-jdk-skip-test
// The case of eisop#2337 without a target type; `var` needs JDK 10. The rest of eisop#2337 is in
// EisopIssue2336.java.

import java.util.List;
import java.util.function.Function;

class EisopIssue2337NoTarget {
    interface Box<X> {
        X get();
    }

    static <T, R> List<R> each(List<T> ts, Function<? super T, ? extends R> f) {
        throw new Error();
    }

    // The outer call has no target type, so its return type variable is still uninstantiated when
    // the lambda is reduced and the inner call is reduced as part of the outer inference problem.
    // The additional argument constraints of the inner call must be known before the return type
    // variable is resolved, or the type argument of the method reference is lost.
    void issue2337NoTarget(List<List<Box<String>>> in) {
        var x = each(in, l -> each(l, Box::get));
        List<List<String>> y = x;
    }
}
