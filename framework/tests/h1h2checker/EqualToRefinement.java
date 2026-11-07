import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;

// Refining an operand of `==`/`!=` must take the greatest lower bound of both operands' values,
// not blindly copy the other operand's value into the store.
// The postcondition check reads the exit store directly, so it observes the stored value
// without the declared type masking it.
public class EqualToRefinement {
    @H1S1 @H2Top Object f;

    void impure() {}

    @EnsuresQualifier(expression = "this.f", qualifier = H1S1.class)
    @EnsuresQualifier(expression = "this.f", qualifier = H2S1.class)
    void refineFieldFromOtherOperand(@H1Top @H2S1 Object x) {
        // Removes the store entry for `this.f`; its value is now only the declared type.
        impure();
        if (x != this.f) {
            throw new RuntimeException();
        }
        // The else store holds x's value for `this.f`: H2S1 is a refinement, but H1Top
        // must not replace the declared H1S1.
    }

    @H1S1 @H2Top Object g;

    // H1S1 and H1S2 are incomparable, so `this.g` and `x` are both H1Bot when they are equal.
    @EnsuresQualifier(expression = "this.g", qualifier = H1S1.class)
    @EnsuresQualifier(expression = "this.g", qualifier = H1S2.class)
    void incomparableQualifiers(@H1S2 @H2Top Object x) {
        if (x != this.g) {
            throw new RuntimeException();
        }
    }

    // Control: without a store entry the postcondition is not provable at all.
    @EnsuresQualifier(expression = "this.f", qualifier = H1S1.class)
    // :: error: (contracts.postcondition.not.satisfied)
    void noStoreEntry() {
        impure();
    }
}
