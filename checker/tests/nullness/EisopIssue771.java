// Test case for https://github.com/eisop/checker-framework/issues/771 :
// the refinement of a @MonotonicNonNull field to @NonNull was forgotten in a lambda.

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

final class EisopIssue771 {
    private @MonotonicNonNull String value = null;

    private @Nullable String nullable = null;

    @MonotonicNonNull EisopIssue771 other = null;

    final EisopIssue771 finalOther = new EisopIssue771(0);

    EisopIssue771(int unused) {}

    @RequiresNonNull("value")
    void print() {
        System.out.println(value);
    }

    void checked() {
        if (value == null) {
            return;
        }
        Runnable r = () -> print();
        Runnable r2 = () -> value.toString();
    }

    void unchecked() {
        // :: error: (contracts.precondition.not.satisfied)
        Runnable r = () -> print();
    }

    void lambdaBeforeCheck() {
        // :: error: (dereference.of.nullable)
        Runnable r = () -> value.toString();
        if (value == null) {
            return;
        }
    }

    void notMonotonic() {
        if (nullable == null) {
            return;
        }
        // :: error: (dereference.of.nullable)
        Runnable r = () -> nullable.toString();
    }

    void finalReceiver() {
        if (finalOther.value == null) {
            return;
        }
        Runnable r = () -> finalOther.value.toString();
    }

    void explicitThis() {
        if (this.value == null) {
            return;
        }
        Runnable r = () -> this.value.toString();
    }

    void localReceiver() {
        EisopIssue771 local = new EisopIssue771(0);
        if (local.other == null) {
            return;
        }
        Runnable r = () -> local.other.toString();
    }

    void nestedLambda() {
        if (value == null) {
            return;
        }
        Runnable r =
                () -> {
                    Runnable inner = () -> value.toString();
                    inner.run();
                };
    }

    void assignableReceiver() {
        if (other == null || other.value == null) {
            return;
        }
        // :: error: (dereference.of.nullable)
        Runnable r = () -> other.value.toString();
    }

    static @MonotonicNonNull String staticValue = null;

    static void staticChecked() {
        if (staticValue == null) {
            return;
        }
        Runnable r = () -> staticValue.toString();
    }
}
