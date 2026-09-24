// Test case for Issue 1217:
// https://github.com/eisop/checker-framework/issues/1217
// Viewpoint adaptation of @NotOnlyInitialized field initializers when receiver is under
// initialization.

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;

public class Issue1217 {
    // Under initialization receiver (this), @NotOnlyInitialized field initializer
    // is adapted to @UnknownInitialization, consistent with assignment in constructor:
    @NotOnlyInitialized Object f1 = this;

    // Normal field (not @NotOnlyInitialized) retains declared type @Initialized,
    // so assigning 'this' is rejected:
    // :: error: (assignment.type.incompatible)
    Object f2 = this;

    @NotOnlyInitialized Issue1217 f3;
    Issue1217 f4;

    Issue1217() {
        // IdentifierTree LHS (implicit this receiver).
        f3 = this;
        // :: error: (assignment.type.incompatible)
        f4 = this;

        // MemberSelectTree LHS with an explicit `this` receiver behaves the same as the
        // bare-identifier LHS above: the getAnnotatedTypeLhs override only special-cases the
        // VariableTree of a field declaration, so this relies on the framework's existing
        // viewpoint adaptation for constructor field writes, not on this fix -- included here
        // as a regression check that the new override did not disturb it.
        this.f3 = this;
        // :: error: (assignment.type.incompatible)
        this.f4 = this;
    }

    // A MemberSelectTree LHS whose receiver is a different, already fully-initialized object
    // (not `this`). Regardless of the field's own @NotOnlyInitialized annotation, storing a
    // value that is still under initialization into a field of an already-initialized object is
    // rejected by a separate invariant (initialization.invalid.field.write.initialized) -- not
    // the diagnostic that assignment.type.incompatible tests elsewhere in this file check for,
    // so it is verified with its own marker.
    Issue1217(boolean unused) {
        Issue1217 other = new Issue1217();
        f3 = other;
        f4 = other;
        // :: error: (initialization.invalid.field.write.initialized)
        other.f3 = this;
        // :: error: (initialization.invalid.field.write.initialized)
        other.f4 = this;
    }
}
