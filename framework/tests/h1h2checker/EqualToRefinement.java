import org.checkerframework.framework.qual.EnsuresQualifier;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Top;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H2Top;

// Where `a == b` or a switch case shows that two values are equal, both operands are refined to
// the greatest lower bound of their values in each hierarchy.
public class EqualToRefinement {
    @H1S1 @H2Top Object f;

    void impure() {}

    // A postcondition check reads the exit store directly, so it observes the stored value without
    // the declared type masking it. Inserting the other operand's value verbatim, while the store
    // had no value for `this.f`, replaced its H1S1 by H1Top.
    @EnsuresQualifier(expression = "this.f", qualifier = H1S1.class)
    @EnsuresQualifier(expression = "this.f", qualifier = H2S1.class)
    void refineFieldFromOtherOperand(@H1Top @H2S1 Object x) {
        // Removes the store entry for `this.f`; its value is now only the declared type.
        impure();
        if (x != this.f) {
            throw new RuntimeException();
        }
    }

    // Control: without a store entry the postcondition is not provable at all.
    @EnsuresQualifier(expression = "this.f", qualifier = H1S1.class)
    // :: error: (contracts.postcondition.not.satisfied)
    void noStoreEntry() {
        impure();
    }

    // H1S1 and H1S2 are incomparable, so both operands are H1Bot where they are equal. Taking the
    // more specific value of the two, with the other operand's qualifier for incomparable ones,
    // gave each operand only the other's qualifier.
    void bothOperandsInThenBranch(@H1S1 @H2Top Object a, @H1S2 @H2Top Object b) {
        if (a == b) {
            @H1Bot @H2Top Object a2 = a;
            @H1Bot @H2Top Object b2 = b;
        }
        // :: error: (assignment.type.incompatible)
        @H1Bot @H2Top Object a3 = a;
    }

    @EnsuresQualifier(expression = "#1", qualifier = H1Bot.class)
    @EnsuresQualifier(expression = "#2", qualifier = H1Bot.class)
    void bothOperandsInElseBranch(@H1S1 @H2Top Object a, @H1S2 @H2Top Object b) {
        if (a != b) {
            throw new RuntimeException();
        }
    }

    // A string literal is H1Top. The constant's declared type is what the case label has.
    // :: error: (assignment.type.incompatible)
    static final @H1S1 @H2Top String S1 = "s1";

    // The selector and the synthetic variable for it are refined by the case label.
    void switchCase(@H1S2 @H2Top String s) {
        switch (s) {
            case S1:
                @H1Bot @H2Top String s2 = s;
                break;
            default:
                // :: error: (assignment.type.incompatible)
                @H1Bot @H2Top String s3 = s;
        }
    }
}
