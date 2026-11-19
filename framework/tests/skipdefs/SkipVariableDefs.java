import org.checkerframework.framework.testchecker.util.SubQual;
import org.checkerframework.framework.testchecker.util.SuperQual;

// SkipDefsChecker skips the variables whose names start with "skipped", and its visitor reports
// type.invalid on each variable it visits whose name ends with "Reported".
public class SkipVariableDefs {

    @SuperQual Object sup;

    // :: error: (assignment.type.incompatible)
    @SubQual Object checkedField = sup;

    @SubQual Object skippedField = sup;

    // A skipped variable's initializer is not checked, including a lambda or class in it.
    Runnable skippedLambda =
            () -> {
                @SubQual Object o = sup;
            };

    Object skippedAnonymous =
            new Object() {
                @SubQual Object f = sup;
            };

    // Skipping a variable also skips an override of visitVariable.
    // :: error: (type.invalid)
    Object checkedReported;

    Object skippedReported;

    // :: error: (type.invalid)
    void locals(@SubQual Object skippedReported, @SubQual Object paramReported) {
        // :: error: (assignment.type.incompatible)
        @SubQual Object checkedLocal = sup;
        @SubQual Object skippedLocal = sup;
    }

    // Code that uses a skipped variable is checked against its declared type.
    void uses() {
        @SubQual Object sub = skippedField;
        // :: error: (assignment.type.incompatible)
        @SubQual Object o = skippedReported;
    }
}
