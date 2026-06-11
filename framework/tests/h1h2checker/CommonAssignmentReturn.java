import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Invalid;

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
}
