import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;

// Refining the second operand of `==`/`!=` must take the most specific value per hierarchy
// from both operands, not blindly copy the other operand's value into the store.
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

    // Control: without a store entry the postcondition is not provable at all.
    @EnsuresQualifier(expression = "this.f", qualifier = H1S1.class)
    // :: error: (contracts.postcondition.not.satisfied)
    void noStoreEntry() {
        impure();
    }
}
