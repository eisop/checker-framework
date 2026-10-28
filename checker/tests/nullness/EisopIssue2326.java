// Test case for https://github.com/eisop/checker-framework/issues/2326 :
// a refinement of a method call must be evicted by a method with side effects whatever the
// receiver and the arguments of the call are, because the value of a call depends on the heap.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class EisopIssue2326 {
    static @Nullable Object sf;

    @Nullable Object f;

    @Pure
    static @Nullable Object pureStatic() {
        return sf;
    }

    @Pure
    @Nullable Object pureInstance() {
        return f;
    }

    @Pure
    static @Nullable Object pureStaticArg(int i) {
        return sf;
    }

    static void clobber() {
        sf = null;
    }

    void clobberInstance() {
        f = null;
    }

    @SuppressWarnings("contracts.postcondition") // the body is irrelevant to this test
    @EnsuresNonNull("pureStatic()")
    static void establishStatic() {}

    void staticCall() {
        if (pureStatic() != null) {
            pureStatic().hashCode();
            clobber();
            // :: error: (dereference.of.nullable)
            pureStatic().hashCode();
        }
    }

    void staticCallWithUnmodifiableArgument() {
        if (pureStaticArg(1) != null) {
            pureStaticArg(1).hashCode();
            clobber();
            // :: error: (dereference.of.nullable)
            pureStaticArg(1).hashCode();
        }
    }

    // The instance method case was already reported.
    void instanceCall() {
        if (pureInstance() != null) {
            pureInstance().hashCode();
            clobberInstance();
            // :: error: (dereference.of.nullable)
            pureInstance().hashCode();
        }
    }

    void contract() {
        establishStatic();
        pureStatic().hashCode();
        clobber();
        // :: error: (dereference.of.nullable)
        pureStatic().hashCode();
    }

    // A side-effect-free call evicts nothing.
    void sideEffectFreeCall() {
        if (pureStatic() != null) {
            pureInstance();
            pureStatic().hashCode();
        }
    }
}
