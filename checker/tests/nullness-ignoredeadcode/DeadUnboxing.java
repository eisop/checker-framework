import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.TerminatesExecution;

import java.util.List;

// Test case for -AignoreDeadCode: the unboxing of a possibly-null value, and the iteration over a
// possibly-null Iterable, are not reported in dead code, but are reported in live code.
class DeadUnboxing {
    @TerminatesExecution
    static void terminate() {
        throw new Error();
    }

    void afterTermination(@Nullable Integer i, @Nullable List<Integer> list) {
        terminate();
        // This code is dead; no error expected under -AignoreDeadCode.
        int x = i;
        for (Integer e : list) {}
    }

    void unreachableCatch(@Nullable Boolean b) {
        try {
            int y = 1;
        } catch (RuntimeException e) {
            // This catch block is dead; no error expected under -AignoreDeadCode.
            if (b) {}
        }
    }

    void live(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        int x = i;
    }
}
