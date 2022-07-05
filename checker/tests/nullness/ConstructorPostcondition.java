import org.checkerframework.checker.nullness.qual.*;

public class ConstructorPostcondition {

    class Box {
        @Nullable Object f;
    }

    // ::error: (contracts.postcondition.not.satisfied)
    @EnsuresNonNull("#1.f")
    ConstructorPostcondition(Box b) {}

    @EnsuresNonNull("#1.f")
    ConstructorPostcondition(Box b, Object o) {
        b.f = o;
    }

    void foo(Box b) {
        ConstructorPostcondition x = new ConstructorPostcondition(b, "x");
        b.f.hashCode();
    }
}
