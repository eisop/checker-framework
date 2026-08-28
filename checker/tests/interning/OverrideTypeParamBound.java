// Regression tests for eisop#1965, checked independently of the Nullness Checker: the
// override-type-parameter-bound check is a framework-level property of
// BaseTypeVisitor.OverrideChecker, not specific to Nullness's qualifier hierarchy.

import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.UnknownInterned;

class OverrideTypeParamBound {

    // Positive control: identical bound, so nothing is reported.
    static class SameBounds {
        <T extends @UnknownInterned Object> void consume(T p) {}
    }

    static class SameBoundsOverride extends SameBounds {
        @Override
        <T extends @UnknownInterned Object> void consume(T p) {}
    }

    // Upper bound narrowed, type parameter used as a parameter. A caller going through the
    // overridden method's declared bound may instantiate the type parameter with an
    // @UnknownInterned value; if the override's own bound requires @Interned, it wrongly gets
    // exercised with a value it cannot safely treat as interned. The parameter occurrence is
    // bare, so isParameterOverrideValid's own type-variable fallback independently reaches the
    // same declared-bound mismatch and reports a second diagnostic.
    static class Super {
        <T extends @UnknownInterned Object> void consume(T p) {}
    }

    static class SubNarrowUpperParam extends Super {
        @Override
        // :: error: (override.typaram.invalid)
        // :: error: (override.param.invalid)
        <T extends @Interned Object> void consume(T p) {}
    }

    static void triggerNarrowUpperParam(Super s) {
        // Not @Interned: a plain "new Object()" is never interned.
        s.<Object>consume(new Object());
    }

    // Positive control: upper bound widened, type parameter used as a return type. What makes
    // this sound is the LOWER bound, not the upper one: the override's body can only manufacture a
    // fresh T from T's lower bound, which containment leaves unchanged (here it defaults to the
    // Interning hierarchy's bottom, @InternedDistinct). Widening the upper bound merely makes the
    // body more conservative about values it was handed; it grants no new ability to produce one.
    // Sound containment, regardless of the type parameter's position.
    static class Super2 {
        <T extends @Interned Object> T produce() {
            throw new RuntimeException();
        }
    }

    static class SubWidenUpperReturn extends Super2 {
        @Override
        <T extends @UnknownInterned Object> T produce() {
            throw new RuntimeException();
        }
    }
}
