// Test case for https://github.com/eisop/checker-framework/issues/2100 :
// a postcondition is a fact about the value computed by the call (its receiver and arguments), so
// it
// must not be kept if the receiver or an argument is a call that is not pure. A contract that
// contains a call is a fact about the next evaluation of that call.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class EisopIssue2100Contracts {

    static class Holder {
        @Nullable Object f = new Object();

        @SideEffectFree
        Holder() {}

        @Pure
        @Nullable Object get() {
            return f;
        }

        @SuppressWarnings("contracts.postcondition") // the body is irrelevant to this test
        @EnsuresNonNull("get()")
        void establishGet() {}

        @Pure
        @Nullable Object pureM() {
            return f;
        }
    }

    // Side-effect-free but not deterministic: not pure, and no side effect to evict anything.
    @SideEffectFree
    Holder nondet() {
        return new Holder();
    }

    @SideEffectFree
    @Nullable Object nondetObject() {
        return null;
    }

    // Deterministic but not side-effect-free: not pure.
    @SuppressWarnings("purity.not.deterministic.object.creation")
    @Deterministic
    static Holder impureStatic() {
        return new Holder();
    }

    @Pure
    static @Nullable Object id(@Nullable Object o) {
        return o;
    }

    @SuppressWarnings("contracts.postcondition")
    @EnsuresNonNull("id(#1)")
    void establishId(@Nullable Object o) {}

    @SuppressWarnings("contracts.postcondition")
    @EnsuresNonNullIf(expression = "#1", result = true)
    boolean isNonNull(@Nullable Object o) {
        return o != null;
    }

    void testImpureReceiverOfContractWithCall() {
        nondet().establishGet();
        // :: error: (dereference.of.nullable)
        nondet().get().hashCode();
    }

    void testPureReceiverOfContractWithCall() {
        Holder h = new Holder();
        h.establishGet();
        h.get().hashCode();
    }

    void testImpureArgumentOfContractWithCall() {
        establishId(nondet());
        // :: error: (dereference.of.nullable)
        id(nondet()).hashCode();
    }

    void testConditionalPostconditionOnImpureArgument() {
        if (isNonNull(nondetObject())) {
            // :: error: (dereference.of.nullable)
            nondetObject().hashCode();
        }
    }

    // The receiver of a call is part of the call: a static call that is not pure is not evicted by
    // a later side effect, so the receiver must be checked for purity.
    void testImpureStaticReceiver() {
        if (impureStatic().pureM() != null) {
            // :: error: (dereference.of.nullable)
            impureStatic().pureM().hashCode();
        }
    }
}
