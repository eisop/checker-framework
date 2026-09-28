import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Test that type arguments of an explicitly-written enclosing type are checked against the
// enclosing type parameter's declared bound, just like the arguments of a direct (non-enclosing)
// parameterized type.  See https://github.com/eisop/checker-framework/issues/737.
abstract class EnclosingTypeArgs {

    static class Min<XXX extends @NonNull Object> {
        class Inner {}
    }

    // Direct (non-enclosing) position: out-of-bound argument is rejected.
    // :: error: (type.argument.type.incompatible)
    void direct(Min<@Nullable String> p) {}

    // Enclosing position: the same out-of-bound argument must also be rejected.
    // :: error: (type.argument.type.incompatible)
    void enclosing(Min<@Nullable String>.Inner p) {}

    // In-bound enclosing argument: no error.
    void okEnclosing(Min<@NonNull String>.Inner p) {}

    // Direct in-bound argument: no error.
    void okDirect(Min<@NonNull String> p) {}

    // Return-type position (validateTypeOf is called with the whole MethodTree, not a type-use
    // tree): the out-of-bound enclosing argument must also be rejected here.
    // :: error: (type.argument.type.incompatible)
    abstract Min<@Nullable String>.Inner returnType();

    // In-bound enclosing argument in return-type position: no error.
    abstract Min<@NonNull String>.Inner okReturnType();

    // Unqualified class instance creation (validateTypeOf is called with the NewClassTree): the
    // out-of-bound enclosing argument must also be rejected here. The enclosing instance ("this")
    // is provided structurally by extending Min<String>; the type argument written on the `new`
    // expression itself is what is being validated here, independent of that receiver.
    static class Sub extends Min<String> {
        void make() {
            // :: error: (type.argument.type.incompatible)
            Min<@Nullable String>.Inner x = new Min<@Nullable String>.Inner();
        }

        // In-bound enclosing argument in the same position: no error.
        void okMake() {
            Min<@NonNull String>.Inner x = new Min<@NonNull String>.Inner();
        }
    }

    // Local variable, enclosing position: the written enclosing argument must reach the
    // validator and be rejected, just like the field/parameter positions above.
    void localVars() {
        // :: error: (type.argument.type.incompatible)
        Min<@Nullable String>.Inner bad = null;
        Min<@NonNull String>.Inner ok = null;
        // Direct (non-enclosing) local for contrast.
        // :: error: (type.argument.type.incompatible)
        Min<@Nullable String> directBad = null;
    }
}

// Extends clause, enclosing position: the written enclosing argument must reach the validator and
// be rejected.  Sub/SubOk are nested so that an enclosing instance of Outer exists.
class Outer<XXX extends @NonNull Object> {
    class Sup {}

    // :: error: (type.argument.type.incompatible)
    class Sub extends Outer<@Nullable String>.Sup {}

    class SubOk extends Outer<@NonNull String>.Sup {}
}
