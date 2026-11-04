// Test case for https://github.com/eisop/checker-framework/issues/2164
// Nested calls to methods with an F-bounded intersection type parameter.  Inference resolves the
// variables of both calls together with fresh type variables, which lost the bound of the inner
// variable by the outer one and kept referring to the declared type variables, so the fresh
// variables were not subtypes of each other.  A failed first resolution attempt also left
// constraints behind that used its instantiations.

import java.time.temporal.Temporal;

public class EisopIssue2164 {
    interface Tem {}

    static <T extends Tem & Comparable<? super T>> T cast(Tem t) {
        throw new Error();
    }

    static <T> T castUnbounded(Tem t) {
        throw new Error();
    }

    static <T extends Tem & Comparable<? super T>> T until(T t) {
        throw new Error();
    }

    Tem nested(Tem t) {
        return until(cast(t));
    }

    Tem nestedUnbounded(Tem t) {
        return until(castUnbounded(t));
    }

    static <T extends Temporal & Comparable<? super T>> T castTemporal(Temporal t) {
        throw new Error();
    }

    static <T extends Temporal & Comparable<? super T>> T elapseUntil(T t, int amount) {
        throw new Error();
    }

    Temporal temporal(Temporal t) {
        return elapseUntil(castTemporal(t), 1);
    }
}
