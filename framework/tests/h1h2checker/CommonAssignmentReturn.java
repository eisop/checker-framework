import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;

public class CommonAssignmentReturn {

    void invalidLhsLocal() {
        // :: error: (type.invalid)
        @H1Invalid Object commonAssignmentInvalid = new Object();
    }

    void invalidLhsAssignment() {
        // :: error: (type.invalid)
        @H1Invalid Object commonAssignmentInvalid;
        // :: error: (type.invalid)
        commonAssignmentInvalid = new Object();
    }

    void invalidRhsAssignment() {
        // :: error: (type.invalid)
        @H1Invalid Object commonAssignmentInvalid = new Object();
        @H1S1 Object target;
        // :: error: (type.invalid)
        target = commonAssignmentInvalid;
    }
}
