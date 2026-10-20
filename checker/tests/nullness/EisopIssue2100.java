// Test case for https://github.com/eisop/checker-framework/issues/2100 :
// a method that is @Deterministic but not @SideEffectFree may modify the heap, so a repeated call
// may return a different value even with no intervening call.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;

public class EisopIssue2100 {
    @Nullable Object f = new Object();

    // Deterministic: given the same arguments and heap, it returns the same value.
    // It is not side-effect-free: it clears the field it returns.
    @SuppressWarnings("purity.not.deterministic.assign.field") // the method is deterministic
    @Deterministic
    @Nullable Object getAndClear() {
        Object o = this.f;
        this.f = null;
        return o;
    }

    @Pure
    @Nullable Object get() {
        return this.f;
    }

    void testDeterministic() {
        if (getAndClear() != null) {
            // :: error: (dereference.of.nullable)
            getAndClear().hashCode();
        }
    }

    void testPure() {
        if (get() != null) {
            get().hashCode();
        }
    }

    void testDeterministicInvalidatesPure() {
        if (get() != null) {
            getAndClear();
            // :: error: (dereference.of.nullable)
            get().hashCode();
        }
    }

    // A precondition about a call is a fact about its next evaluation only.
    @RequiresNonNull("getAndClear()")
    void testContractUsedOnce() {
        getAndClear().hashCode();
        // :: error: (dereference.of.nullable)
        getAndClear().hashCode();
    }
}
