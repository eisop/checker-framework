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

    // Positive control: upper bound widened, type parameter used as a return type. The override's
    // body is checked once against its own (wider) upper bound and cannot manufacture a value
    // outside it, so every value it returns still satisfies Super2's narrower upper bound --
    // sound containment, regardless of the type parameter's position.
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
