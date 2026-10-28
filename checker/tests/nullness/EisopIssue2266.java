// Test case for https://github.com/eisop/checker-framework/issues/2266 :
// the type arguments of an inner class type include those of its enclosing types (JLS 4.5), so
// the type hierarchy must compare them, too.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class EisopIssue2266 {
    static class Outer<T extends @Nullable Object> {
        class Inner {}

        class Gen<U extends @Nullable Object> {}

        class Mid<U extends @Nullable Object> {
            class Deep {}
        }

        // The implicit enclosing type of Inner is Outer<T>.
        Inner implicit(Inner i, Outer<T>.Inner explicit) {
            Inner a = explicit;
            Outer<T>.Inner b = i;
            return this.new Inner();
        }
    }

    void assign(Outer<@Nullable String>.Inner n, Outer<@NonNull String>.Inner nn) {
        // :: error: (assignment.type.incompatible)
        Outer<@NonNull String>.Inner a = n;
        // :: error: (assignment.type.incompatible)
        Outer<@Nullable String>.Inner b = nn;
        Outer<@Nullable String>.Inner c = n;
        Outer<@NonNull String>.Inner d = nn;
    }

    void takeNonNull(Outer<@NonNull String>.Inner p) {}

    void takeNullable(Outer<@Nullable String>.Inner p) {}

    void arguments(Outer<@Nullable String>.Inner n, Outer<@NonNull String>.Inner nn) {
        // :: error: (argument.type.incompatible)
        takeNonNull(n);
        // :: error: (argument.type.incompatible)
        takeNullable(nn);
        takeNullable(n);
        takeNonNull(nn);
    }

    void wildcards(Outer<@Nullable String>.Inner n, Outer<@NonNull String>.Inner nn) {
        Outer<? extends @Nullable String>.Inner a = n;
        Outer<? extends @Nullable String>.Inner b = nn;
        // :: error: (assignment.type.incompatible)
        Outer<? extends @NonNull String>.Inner c = n;
        Outer<? extends @NonNull String>.Inner d = nn;
        Outer<? super @NonNull String>.Inner e = n;
        // :: error: (assignment.type.incompatible)
        Outer<? super @Nullable String>.Inner f = nn;
    }

    void ownTypeArgs(Outer<@Nullable String>.Gen<String> n, Outer<@NonNull String>.Gen<String> nn) {
        // :: error: (assignment.type.incompatible)
        Outer<@NonNull String>.Gen<String> a = n;
        // :: error: (assignment.type.incompatible)
        Outer<@Nullable String>.Gen<String> b = nn;
    }

    void deep(
            Outer<@Nullable String>.Mid<String>.Deep n,
            Outer<@NonNull String>.Mid<String>.Deep nn,
            Outer<@NonNull String>.Mid<@Nullable String>.Deep mn) {
        // :: error: (assignment.type.incompatible)
        Outer<@NonNull String>.Mid<String>.Deep a = n;
        // :: error: (assignment.type.incompatible)
        Outer<@Nullable String>.Mid<String>.Deep b = nn;
        // :: error: (assignment.type.incompatible)
        Outer<@NonNull String>.Mid<String>.Deep c = mn;
        Outer<@NonNull String>.Mid<String>.Deep d = nn;
    }

    void invariant(List<Outer<@Nullable String>.Inner> n, List<Outer<@NonNull String>.Inner> nn) {
        // :: error: (assignment.type.incompatible)
        List<Outer<@NonNull String>.Inner> a = n;
        // :: error: (assignment.type.incompatible)
        List<Outer<@Nullable String>.Inner> b = nn;
        List<Outer<@Nullable String>.Inner> c = n;
        List<Outer<@NonNull String>.Inner> d = nn;
    }

    static class SubOuter extends Outer<@NonNull String> {
        class SubInner extends Inner {}
    }

    void subclass(SubOuter so) {
        Outer<@NonNull String>.Inner a = so.new SubInner();
        // :: error: (assignment.type.incompatible)
        Outer<@Nullable String>.Inner b = so.new SubInner();
    }

    void anonymous(Outer<@Nullable String> n, Outer<@NonNull String> nn) {
        Outer<@Nullable String>.Inner a = n.new Inner() {};
        Outer<@NonNull String>.Inner b = nn.new Inner() {};
        // :: error: (assignment.type.incompatible)
        Outer<@NonNull String>.Inner c = n.new Inner() {};
        // :: error: (assignment.type.incompatible)
        Outer<@Nullable String>.Inner d = nn.new Inner() {};
    }

    static <F extends @Nullable Object> void sameArg(Outer<F>.Inner a, F f) {}

    static <F extends @Nullable Object> void extendsArg(Outer<? extends F>.Inner a, F f) {}

    static <F extends @Nullable Object> F get(Outer<? extends F>.Inner a) {
        throw new Error();
    }

    static <F extends @Nullable Object> Outer<F>.Inner make(F f) {
        throw new Error();
    }

    // Type argument inference must compare the qualifiers of enclosing type arguments, too, to
    // be consistent with the type hierarchy.
    void inference(
            Outer<@Nullable String>.Inner n,
            Outer<@NonNull String>.Inner nn,
            @NonNull String s,
            @Nullable String ns) {
        sameArg(n, s);
        sameArg(nn, s);
        // :: error: (type.arguments.not.inferred)
        sameArg(nn, ns);
        extendsArg(n, s);
        extendsArg(nn, ns);
        @Nullable String a = get(n);
        @NonNull String b = get(nn);
        // :: error: (assignment.type.incompatible) :: error: (type.arguments.not.inferred)
        @NonNull String c = get(n);
        Outer<@NonNull String>.Inner d = make(s);
        Outer<@Nullable String>.Inner e = make(s);
        // :: error: (type.arguments.not.inferred)
        Outer<@NonNull String>.Inner f = make(ns);
    }

    @SuppressWarnings("unchecked") // raw types
    void raw(Outer.Inner r) {
        Outer<@NonNull String>.Inner a = r;
        Outer<@Nullable String>.Inner b = r;
    }
}
