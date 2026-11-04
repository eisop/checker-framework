// Test case for issue 720: viewpoint adaptation of @NotOnlyInitialized fields
// https://github.com/eisop/checker-framework/issues/720

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class NotOnlyInitializedAdaptation {
    @NotOnlyInitialized Object f = new Object();
    final @NotOnlyInitialized Object finalF = new Object();
    Object normal = new Object();

    void fieldAccess1(@UnderInitialization NotOnlyInitializedAdaptation this) {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @UnderInitialization.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        f.hashCode();

        @UnknownInitialization Object u = f;
        // :: error: (assignment.type.incompatible)
        @Initialized Object i = f;
    }

    void fieldAccess2(@UnknownInitialization NotOnlyInitializedAdaptation this) {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @UnknownInitialization.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        f.hashCode();

        @UnknownInitialization Object u = f;
        // :: error: (assignment.type.incompatible)
        @Initialized Object i = f;
    }

    void fieldAccess3() {
        // @NotOnlyInitialized should be correctly adapted to @Initialized by @Initialized.
        // This is the only way to enter the then branch in the issue.
        // The correct adaptation ensures the correct use of @NotOnlyInitialized.
        f.hashCode();

        @Initialized Object i = f;
    }

    // Test #712: field access across method calls (CFAbstractStore vs InitializationStore).
    void fieldAccessAcrossMethodCalls() {
        f.hashCode();
        sideEffect();
        // Accessing @NotOnlyInitialized field on @Initialized receiver after a method call
        // should still have adapted type @Initialized without false positive:
        f.hashCode();
        @Initialized Object i = f;
    }

    void fieldRefinementAcrossMethodCalls(@UnderInitialization NotOnlyInitializedAdaptation this) {
        // Assign an @Initialized object to f: in the store, f is now @Initialized.
        this.f = new Object();
        // Calling a method clears mutable field values from the store.
        sideEffectUnderInit();
        // Since this is @UnderInitialization, f reverts to adapted declared type
        // @UnknownInitialization:
        // :: error: (method.invocation.invalid)
        this.f.hashCode();

        // But normal fields (not @NotOnlyInitialized) remain @Initialized on @UnderInitialization
        // receiver:
        this.normal = new Object();
        sideEffectUnderInit();
        this.normal.hashCode();

        // Final @NotOnlyInitialized fields preserve their refined value across method calls:
        sideEffectUnderInit();
        this.finalF.hashCode();
    }

    @NotOnlyInitialized NotOnlyInitializedAdaptation noiField;

    void testFieldWrites(
            @Initialized NotOnlyInitializedAdaptation initReceiver,
            @UnderInitialization NotOnlyInitializedAdaptation underInitReceiver,
            @UnderInitialization NotOnlyInitializedAdaptation underInitVal) {
        // Storing under-initialization value into @NotOnlyInitialized field on under-initialization
        // receiver is allowed:
        underInitReceiver.noiField = underInitVal;

        // Storing under-initialization value into normal field is rejected:
        // :: error: (assignment.type.incompatible)
        underInitReceiver.normal = underInitVal;

        // Storing under-initialization value into @NotOnlyInitialized field on @Initialized
        // receiver is forbidden:
        // :: error: (initialization.invalid.field.write.initialized)
        initReceiver.noiField = underInitVal;
    }

    void sideEffect() {}

    void sideEffectUnderInit(@UnderInitialization NotOnlyInitializedAdaptation this) {}

    NotOnlyInitializedAdaptation() {
        noiField = this;
    }
}
