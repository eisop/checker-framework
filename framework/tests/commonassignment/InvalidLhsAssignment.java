import org.checkerframework.framework.testchecker.commonassignment.quals.CommonInvalid;

/**
 * Regression test for PR #736: when {@code validateType} fails for the LHS of an assignment, {@code
 * BaseTypeVisitor.commonAssignmentCheck(Tree, ExpressionTree, String, Object[])} must return {@code
 * false}, not {@code true}. Otherwise, subclass overrides that compose the parent's result with
 * their own check via {@code &&} would silently treat the assignment as valid.
 *
 * <p>The {@code CommonAssignmentVisitor} in this test issues the secondary warning {@code
 * commonassignment.parent.succeeded} only when {@code super.commonAssignmentCheck} returns {@code
 * true}. Since {@code @CommonInvalid} is reported as an invalid type by the test validator, the
 * super call must return {@code false} and that secondary warning must not be issued.
 */
public class InvalidLhsAssignment {

    void invalidLhsLocal() {
        // The variable's declared type is invalid; the framework reports "type.invalid".
        // commonAssignmentCheck must NOT additionally issue "commonassignment.parent.succeeded".
        // :: error: (type.invalid)
        @CommonInvalid Object o = new Object();
    }

    void invalidLhsAssignment() {
        // The variable declaration alone reports "type.invalid" (no initializer).
        // :: error: (type.invalid)
        @CommonInvalid Object o;
        // The LHS type is invalid; the same property must hold for the assignment expression.
        // :: error: (type.invalid)
        o = new Object();
    }
}
