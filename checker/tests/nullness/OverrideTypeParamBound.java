// Regression tests for eisop#1965: a generic method override whose own declared type-parameter
// bound no longer contains the overridden method's bound is unsound regardless of whether, or
// where, the type parameter is used in the parameter or return types -- see BaseTypeVisitor
// .OverrideChecker#isTypeParameterBoundOverrideValid's javadoc for the containment rule. Each
// nested class below isolates one combination of which bound differs, in which direction, and how
// the type parameter is used; several combinations are sound and expect no diagnostic, since
// containment (not equality) is position-independent already.
//
// The core matrix is 2 (which bound: upper/lower) x 2 (direction: widened/narrowed) x 2 (position:
// parameter-only/return-only):
//
//   upper  widened, parameter-only: SubImplicitBound      sound, accepted
//   upper  widened, return-only:    SubWidenUpperReturn   sound, accepted
//   upper narrowed, parameter-only: SubNarrowUpperParam   unsound, rejected
//   upper narrowed, return-only:    SubNarrowUpperReturn  sound for this shape, rejected
//   lower  widened, parameter-only: SubWidenLowerParam    unsound, rejected
//   lower  widened, return-only:    SubWidenLowerReturn   unsound, rejected
//   lower narrowed, parameter-only: SubNarrowLowerParam   sound, accepted
//   lower narrowed, return-only:    SubNarrowLowerReturn  sound, accepted
//
// Throughout, the operative asymmetry is that a type parameter's UPPER bound governs what the
// overriding body may CONSUME (dereference, pass on) and its LOWER bound governs what that body
// may PRODUCE (manufacture as a fresh value of the type parameter). That is why widening the
// upper bound is always safe -- it only makes the body more conservative about what it received
// -- while widening the lower bound is not: it lets the body conjure a value that a caller going
// through the overridden, narrower declaration never agreed to receive.
//
// The final section below (Super9 through Super12) instead keeps both sides' type-parameter
// declarations identical and requalifies a single parameter or return occurrence with its own
// explicit annotation -- a containment mismatch entirely invisible to
// isTypeParameterBoundOverrideValid, caught only by isParameterOverrideValid/
// isReturnOverrideValid's own type-variable fallback.

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
    // The override's own body relies on its (wider) lower bound to justify returning the null it
    // put in "t" -- the declaration "T t = null;" alone is accepted either way, since dataflow
    // refines the local; it is "return t;" that the narrower lower bound would reject. But a
    // caller of the overridden signature may instantiate T as @NonNull, per Super's (narrower)
    // lower bound -- an NPE the checker is supposed to prevent. Containment requires the
    // override's lower bound to be a subtype of (i.e. at least as restrictive as) the overridden
    // one, so widening it is still rejected. This return type occurrence is bare (no explicit
    // annotation of its own), so isReturnOverrideValid's own type-variable fallback independently
    // reaches the same, now-widened, declared lower bound and reports a second diagnostic.

    static class Super {
        <T extends @Nullable Object> T pick(T p) {
            return p;
        }
    }

    static class SubWidenLowerReturn extends Super {
        @Override
        // :: error: (override.typaram.invalid)
        // :: error: (override.return.invalid)
        <@Nullable T extends @Nullable Object> T pick(T p) {
            T t = null;
            return t;
        }
    }

    private static void triggerWidenLowerReturn(Super s) {
        @NonNull String r = s.<@NonNull String>pick("x");
        System.out.println(r.length());
    }

    // ---- Upper bound narrowed, type parameter used as a parameter. ----
    // A caller of Super2's declared signature may pass an @Nullable value; SubNarrowUpperParam's
    // own upper bound only accepts @NonNull. Containment requires the overridden upper bound to
    // be a subtype of the override's, so narrowing is rejected. The parameter occurrence is bare,
    // so isParameterOverrideValid's own type-variable fallback independently reaches the same
    // declared-bound mismatch and reports a second diagnostic.

    static class Super2 {
        <T extends @Nullable Object> void consume(T p) {}
    }

    static class SubNarrowUpperParam extends Super2 {
        @Override
        // :: error: (override.typaram.invalid)
        // :: error: (override.param.invalid)
        <T extends @NonNull Object> void consume(T p) {}
    }

    // ---- Positive control: upper bound widened, type parameter used as a return type. ----
    // What makes this sound is the LOWER bound, not the upper one: a body cannot manufacture a
    // fresh value of T except from T's lower bound, and containment leaves that bound unchanged
    // here (both sides default to @NonNull). Widening the upper bound only makes the body more
    // conservative about values it was handed; it grants no new ability to produce one. Note that
    // "the body cannot manufacture a value outside its own (wider) upper bound" would NOT justify
    // this: such a value is exactly what the wider bound does allow, yet returning it is still
    // rejected -- writing "@Nullable T t = someNullableT(); return t;" here fails with
    // return.type.incompatible, whose printed types differ only in the lower bound
    // ("super @Nullable NullType" vs "super @NonNull NullType"), the upper bounds being identical.
    // Widening the upper bound is sound containment regardless of the type parameter's position.

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
    // The lower bound governs what the body may PRODUCE, not what a caller may supply (that is
    // the upper bound's job). SubNarrowLowerParam's own lower bound is more restrictive than
    // Super4's, so its body can manufacture strictly fewer values of T than Super4's body could
    // -- every one of them still acceptable to a caller of Super4's declared signature. Narrowing
    // the lower bound is therefore sound containment.

    static class Super4 {
        <@Nullable T extends @Nullable Object> void consume(T p) {}
    }

    static class SubNarrowLowerParam extends Super4 {
        @Override
        <T extends @Nullable Object> void consume(T p) {}
    }

    // ---- Positive control: lower bound narrowed, type parameter used only as a return type. ----
    // The mirror image of SubNarrowLowerParam, completing the lower-bound half of the matrix.
    // Narrowing the lower bound restricts what SubNarrowLowerReturn's body may produce, and the
    // return type is precisely where that production becomes visible to a caller: a caller of
    // Super14's declared signature is promised a T within Super14's (wider) lower bound, and any
    // value satisfying the override's narrower one satisfies that promise too. Sound containment.

    static class Super14 {
        <@Nullable T extends @Nullable Object> T produce() {
            throw new RuntimeException();
        }
    }

    static class SubNarrowLowerReturn extends Super14 {
        @Override
        <T extends @Nullable Object> T produce() {
            throw new RuntimeException();
        }
    }

    // ---- Lower bound widened, type parameter used only as a parameter. ----
    // Completing the matrix's last cell. It is tempting to expect the mirror image of
    // SubNarrowUpperReturn below -- "the lower bound only matters for what the body produces, and
    // a parameter-only type parameter produces nothing, so this is sound in principle and merely
    // rejected by position-independence." That reasoning is wrong, and Super16 below shows why:
    // "parameter-only" does not mean "produces nothing", because a parameter can itself be a
    // mutable SINK for T. So this cell is genuinely unsound in general, not accepted imprecision.
    //
    // For this bare shape alone, nothing the body manufactures can escape, so this particular
    // rejection is imprecise; the check cannot distinguish it from Super16's shape, and rejects
    // both. Note that only checkTypeParameterBounds catches this: unlike every upper-bound
    // mismatch above, the bare parameter occurrence draws no second diagnostic, because the
    // occurrence-level subtype check on two corresponding type variables compares their effective
    // upper bounds and never reaches the differing lower bound.

    static class Super15 {
        <T extends @Nullable Object> void consume(T p) {}
    }

    static class SubWidenLowerParam extends Super15 {
        @Override
        // :: error: (override.typaram.invalid)
        <@Nullable T extends @Nullable Object> void consume(T p) {}
    }

    // The escape that makes the cell above genuinely unsound rather than merely imprecise. T
    // occurs only in parameter position here too, but one of those parameters is a List<T> the
    // caller still holds. The widened lower bound lets the body manufacture a null T and store it
    // there, so a caller of Super16's declared signature -- which promised a List<@NonNull String>
    // would only ever gain @NonNull String elements -- reads back a null. Note the body's
    // "sink.add(manufactured)" is itself accepted: the override checks are the only thing standing
    // between this declaration and the NPE in triggerWidenLowerParamSink.

    static class Super16 {
        <T extends @Nullable Object> void consume(T p, List<T> sink) {}
    }

    static class SubWidenLowerParamSink extends Super16 {
        @Override
        // :: error: (override.typaram.invalid)
        // :: error: (override.param.invalid)
        <@Nullable T extends @Nullable Object> void consume(T p, List<T> sink) {
            T manufactured = null;
            sink.add(manufactured);
        }
    }

    private static void triggerWidenLowerParamSink(Super16 s) {
        List<@NonNull String> l = new java.util.ArrayList<>();
        s.<@NonNull String>consume("x", l);
        for (@NonNull String e : l) {
            System.out.println(e.length());
        }
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
    // Confirms checkTypeParameterBounds compares type parameters positionally -- diagnostics only
    // for the mismatched index, none for the matching one -- not one verdict for the whole
    // method. U's parameter occurrence is bare, so it draws a second diagnostic the same way the
    // single-type-parameter cases above do; T's occurrence draws none, since T's own bound did
    // not change.

    static class Super8 {
        <T extends @Nullable Object, U extends @Nullable Object> void consumeTwo(T p1, U p2) {}
    }

    static class SubOnlySecondTypeParamMismatched extends Super8 {
        @Override
        <
                        T extends @Nullable Object,
                        // :: error: (override.typaram.invalid)
                        U extends @NonNull Object>
                // :: error: (override.param.invalid)
                void consumeTwo(T p1, U p2) {}
    }

    // ---- Accepted imprecision: upper bound narrowed, type parameter used only as a return type.
    // ----
    // Sound for this particular shape -- T occurs only as a bare return type, so the body can
    // never be handed a value outside its own narrower bound -- but checkTypeParameterBounds is
    // deliberately position-independent (it cannot know, from the declaration alone, whether T is
    // used as a parameter, a return type, both, or neither) and rejects this anyway. The converse,
    // SubWidenUpperReturn above, is the direction position-independence costs nothing for; this is
    // the direction it costs precision.
    //
    // "Return-only" is not by itself enough to make narrowing the upper bound safe, though, just
    // as "parameter-only" was not enough to make widening the lower bound safe in
    // SubWidenLowerParam above. Had Super13 declared "List<T> produce()", the override would hand
    // the caller a container it still believes has @NonNull elements while the caller may add
    // null to it -- genuinely unsound, and rejected by the nested return-type check as well as by
    // this one. So the imprecision here is narrower than "any return-only narrowing is fine".

    static class Super13 {
        <T extends @Nullable Object> T produce() {
            throw new RuntimeException();
        }
    }

    static class SubNarrowUpperReturn extends Super13 {
        @Override
        // :: error: (override.typaram.invalid)
        <T extends @NonNull Object> T produce() {
            throw new RuntimeException();
        }
    }

    // ---- Requalified occurrences: an explicit annotation on a parameter or return type, not on
    // the type parameter's own declaration. Both type parameters below declare the identical bound
    // <T extends @Nullable Object>, so checkTypeParameterBounds sees no mismatch; the unsoundness
    // is entirely in the requalified occurrence, which only isParameterOverrideValid/
    // isReturnOverrideValid's own type-variable fallback can see.

    // Parameter requalified narrower than the shared declared bound: a caller of Super9's declared
    // signature may pass an @Nullable value; SubRequalifiedParamNarrower's own parameter only
    // accepts @NonNull, then dereferences it unconditionally.
    static class Super9 {
        <T extends @Nullable Object> void m(T p) {}
    }

    static class SubRequalifiedParamNarrower extends Super9 {
        @Override
        // :: error: (override.param.invalid)
        <T extends @Nullable Object> void m(@NonNull T p) {
            p.toString();
        }
    }

    private static void triggerRequalifiedParamNarrower(Super9 s) {
        s.<@Nullable String>m(null);
    }

    // Positive control: parameter requalified wider than the shared declared bound. Super10's own
    // parameter already only accepts @NonNull; SubRequalifiedParamWider accepts the full
    // (unrestricted, @Nullable) range T allows, a strictly weaker requirement -- sound, since every
    // value a caller of Super10's declared signature could pass is also accepted here.
    static class Super10 {
        <T extends @Nullable Object> void m(@NonNull T p) {}
    }

    static class SubRequalifiedParamWider extends Super10 {
        @Override
        <T extends @Nullable Object> void m(T p) {}
    }

    // Return requalified wider than the shared declared bound: a caller of Super11's declared
    // signature (via an explicit type witness) expects a @NonNull result; SubRequalifiedReturnWider
    // always returns @Nullable, regardless of the type argument the caller chose.
    static class Super11 {
        <T extends @Nullable Object> T get(T p) {
            return p;
        }
    }

    static class SubRequalifiedReturnWider extends Super11 {
        @Override
        // :: error: (override.return.invalid)
        <T extends @Nullable Object> @Nullable T get(T p) {
            return null;
        }
    }

    private static void triggerRequalifiedReturnWider(Super11 s) {
        @NonNull String r = s.<@NonNull String>get("x");
        System.out.println(r.length());
    }

    // Positive control: return requalified narrower than the shared declared bound. Super12's own
    // return is already the full (unrestricted, @Nullable) range T allows;
    // SubRequalifiedReturnNarrower always returns @NonNull, a strictly stronger guarantee -- sound,
    // since a @NonNull value always satisfies whatever @Nullable result a caller of Super12's
    // declared signature expects.
    static class Super12 {
        <T extends @Nullable Object> @Nullable T get() {
            throw new RuntimeException();
        }
    }

    static class SubRequalifiedReturnNarrower extends Super12 {
        @Override
        <T extends @Nullable Object> @NonNull T get() {
            throw new RuntimeException();
        }
    }

    // Runs every trigger*() method above, each demonstrating -- if the preceding class's own
    // override were accepted rather than rejected -- the NullPointerException a caller relying on
    // the overridden (wider) signature would hit.
    public static void main(String[] args) {
        triggerWidenLowerReturn(new SubWidenLowerReturn());
        triggerWidenLowerParamSink(new SubWidenLowerParamSink());
        triggerRequalifiedParamNarrower(new SubRequalifiedParamNarrower());
        triggerRequalifiedReturnWider(new SubRequalifiedReturnWider());
    }
}
