// Regression tests for eisop#1965: a generic method override whose own declared type-parameter
// bound no longer contains the overridden method's bound is unsound regardless of whether, or
// where, the type parameter is used in the parameter or return types -- see BaseTypeVisitor
// .OverrideChecker#isTypeParameterBoundOverrideValid's javadoc for the containment rule. Each
// nested class below isolates one combination of which bound differs, in which direction, and how
// the type parameter is used; several combinations are sound and expect no diagnostic, since
// containment (not equality) is position-independent already.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class OverrideTypeParamBound {

    // ---- Positive controls: no bound difference, so nothing is reported. ----

    static class SameBoundsExplicit {
        <T extends @Nullable Object> T pick(T p) {
            return p;
        }
    }

    static class SameBoundsExplicitOverride extends SameBoundsExplicit {
        @Override
        <T extends @Nullable Object> T pick(T p) {
            return p;
        }
    }

    static class SameBoundsRenamed extends SameBoundsExplicit {
        @Override
        // A differently-named type parameter with the identical bound is still a valid override:
        // only the bound is compared, never the name.
        <U extends @Nullable Object> U pick(U p) {
            return p;
        }
    }

    // ---- Positive control: an unannotated bound differs only through defaulting. ----
    // An unbounded <T> defaults to a @Nullable Object upper bound, while <T extends Object>
    // defaults to @NonNull Object; overriding with the wider (unbounded) upper bound is sound
    // containment even though neither declaration writes an explicit qualifier.

    static class SuperExplicitObjectBound {
        <T extends Object> void consume(T p) {}
    }

    static class SubImplicitBound extends SuperExplicitObjectBound {
        @Override
        <T> void consume(T p) {}
    }

    // ---- The original eisop#1965 case: lower bound widened, type parameter used as return. ----
    // The override's own body relies on its (wider) lower bound to justify "T t = null;", but a
    // caller of the overridden signature may instantiate T as @NonNull, per Super's (narrower)
    // lower bound -- an NPE the checker is supposed to prevent. Containment requires the
    // override's lower bound to be a subtype of (i.e. at least as restrictive as) the overridden
    // one, so widening it is still rejected.

    static class Super {
        <T extends @Nullable Object> T pick(T p) {
            return p;
        }
    }

    static class SubWidenLowerReturn extends Super {
        @Override
        // :: error: (override.typaram.invalid)
        <@Nullable T extends @Nullable Object> T pick(T p) {
            T t = null;
            return t;
        }
    }

    private static void triggerWidenLowerReturn(Super s) {
        @NonNull String r = s.<@NonNull String>pick("x");
        System.out.println(r.length());
    }

    public static void main(String[] args) {
        triggerWidenLowerReturn(new SubWidenLowerReturn());
    }

    // ---- Upper bound narrowed, type parameter used as a parameter. ----
    // A caller of Super2's declared signature may pass an @Nullable value; SubNarrowUpperParam's
    // own upper bound only accepts @NonNull. Containment requires the overridden upper bound to
    // be a subtype of the override's, so narrowing is rejected.

    static class Super2 {
        <T extends @Nullable Object> void consume(T p) {}
    }

    static class SubNarrowUpperParam extends Super2 {
        @Override
        // :: error: (override.typaram.invalid)
        <T extends @NonNull Object> void consume(T p) {}
    }

    // ---- Positive control: upper bound widened, type parameter used as a return type. ----
    // SubWidenUpperReturn's body is checked once against its own (wider) upper bound; it cannot
    // manufacture a value outside what that bound allows, so every value it returns is still a
    // genuine instance of Super3's narrower upper bound. Widening the upper bound is sound
    // containment regardless of the type parameter's position.

    static class Super3 {
        <T extends @NonNull Object> T produce() {
            throw new RuntimeException();
        }
    }

    static class SubWidenUpperReturn extends Super3 {
        @Override
        // Throws rather than "return null;": T's own lower bound (still defaults to @NonNull
        // here) would make that its own, unrelated error, muddying a test that is meant to
        // isolate the upper bound alone.
        <T extends @Nullable Object> T produce() {
            throw new RuntimeException();
        }
    }

    // ---- Positive control: lower bound narrowed, type parameter used as a parameter. ----
    // SubNarrowLowerParam's own lower bound is more restrictive than Super4's; every value a
    // caller can supply under Super4's declared signature also satisfies the narrower bound, so
    // narrowing the lower bound is sound containment.

    static class Super4 {
        <@Nullable T extends @Nullable Object> void consume(T p) {}
    }

    static class SubNarrowLowerParam extends Super4 {
        @Override
        <T extends @Nullable Object> void consume(T p) {}
    }

    // ---- Both bounds differ at once: still exactly one diagnostic, for the one type parameter.
    // ----

    static class Super5 {
        <T extends @Nullable Object> T pick(T p) {
            return p;
        }
    }

    static class SubBothBoundsDiffer extends Super5 {
        @Override
        // :: error: (override.typaram.invalid)
        <@Nullable T extends @NonNull Object> T pick(T p) {
            throw new RuntimeException();
        }
    }

    // ---- Type parameter not used in either the parameter or the return type. ----
    // No per-occurrence check (isParameterOverrideValid/isReturnOverrideValid) ever sees T here;
    // only checkTypeParameterBounds can catch this.

    static class Super6 {
        <T extends @Nullable Object> void noop() {}
    }

    static class SubUnusedTypeParam extends Super6 {
        @Override
        // :: error: (override.typaram.invalid)
        <T extends @NonNull Object> void noop() {}
    }

    // ---- Type parameter used only nested inside a parameter type, not bare. ----
    // Unlike every case above, this reports two diagnostics, not one: the per-occurrence
    // type-variable check that isParameterOverrideValid falls back to only ever applies to a bare
    // occurrence, never to a type parameter nested inside List<T>. The ordinary parameter-type
    // subtype check, which does see this nested occurrence, independently rejects it too.

    static class Super7 {
        <T extends @Nullable Object> void consumeList(List<T> p) {}
    }

    static class SubNestedTypeParam extends Super7 {
        @Override
        // :: error: (override.typaram.invalid)
        // :: error: (override.param.invalid)
        <T extends @NonNull Object> void consumeList(List<T> p) {}
    }

    // ---- Multiple type parameters: only the second one's bound differs. ----
    // Confirms checkTypeParameterBounds compares type parameters positionally -- one diagnostic
    // for the mismatched index, none for the matching one -- not one verdict for the whole
    // method.

    static class Super8 {
        <T extends @Nullable Object, U extends @Nullable Object> void consumeTwo(T p1, U p2) {}
    }

    static class SubOnlySecondTypeParamMismatched extends Super8 {
        @Override
        <
                        T extends @Nullable Object,
                        // :: error: (override.typaram.invalid)
                        U extends @NonNull Object>
                void consumeTwo(T p1, U p2) {}
    }
}
