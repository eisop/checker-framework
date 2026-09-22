// test case for issue 720
// https://github.com/eisop/checker-framework/issues/720

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class Issue720 {
    @NotOnlyInitialized Object f = new Object();
    final @NotOnlyInitialized Object finalF = new Object();
    Object normal = new Object();

    void fieldAccess1(@UnderInitialization Issue720 this) {
        // @NotOnlyInitialized should be correctly adapted to @UnknownInitialization
        // by @UnderInitialization.
        // :: error: (dereference.of.nullable) :: error: (method.invocation.invalid)
        f.hashCode();

        @UnknownInitialization Object u = f;
        // :: error: (assignment.type.incompatible)
        @Initialized Object i = f;
    }

    void fieldAccess2(@UnknownInitialization Issue720 this) {
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
        // This is the only way to enter then branch in the issue. The correct adaption ensures the
        // correct use of @NotOnlyInitialized.
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

    void fieldRefinementAcrossMethodCalls(@UnderInitialization Issue720 this) {
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

    void testFieldWrites(
            @UnderInitialization Issue720 underInitReceiver,
            @UnderInitialization Object underInitVal) {
        // Storing under-initialization value into @NotOnlyInitialized field on under-initialization
        // receiver is allowed:
        underInitReceiver.f = underInitVal;

        // Storing under-initialization value into normal field is rejected:
        // :: error: (assignment.type.incompatible)
        underInitReceiver.normal = underInitVal;

        // Storing under-initialization value into @NotOnlyInitialized field on @Initialized
        // receiver is forbidden:
        // :: error: (initialization.invalid.field.write.initialized)
        this.f = underInitVal;
    }

    void sideEffect() {}

    void sideEffectUnderInit(@UnderInitialization Issue720 this) {}

    // False positive (#1217): The initializer should be consistent with constructor.
    // The LHS should be adapted to @UnknownInitialization instead of Initialized.
    // :: error: (assignment.type.incompatible)
    @NotOnlyInitialized Object g = this;
    @NotOnlyInitialized Object h;

    Issue720() {
        h = this;
    }
}
