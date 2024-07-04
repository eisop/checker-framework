// Test multiple throw excetion in method signature for issue 363:
// https://github.com/eisop/checker-framework/issues/363

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class EISOPIssue363b {
    class MyException1 extends Exception {
        @NotOnlyInitialized EISOPIssue363b cause;

        MyException1(EISOPIssue363b cause) {
            this.cause = cause;
        }

        MyException1(@UnderInitialization EISOPIssue363b cause, String dummy) {
            this.cause = cause;
        }

        MyException1(@UnknownInitialization EISOPIssue363b cause, int dummy) {
            this.cause = cause;
        }
    }

    class MyException2 extends Exception {
        @NotOnlyInitialized EISOPIssue363b cause;

        MyException2(EISOPIssue363b cause) {
            this.cause = cause;
        }

        MyException2(@UnderInitialization EISOPIssue363b cause, String dummy) {
            this.cause = cause;
        }

        MyException2(@UnknownInitialization EISOPIssue363b cause, int dummy) {
            this.cause = cause;
        }
    }

    Object field;

    EISOPIssue363b(int dummy) throws MyException1, MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException1(this, 0);
    }

    EISOPIssue363b(int dummy1, int dummy2) throws MyException1, MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException2(this, 0);
    }

    EISOPIssue363b(boolean dummy) throws @UnknownInitialization MyException1, MyException2 {
        throw new MyException1(this, 0);
    }

    EISOPIssue363b(boolean dummy1, boolean dummy2)
            throws @UnknownInitialization MyException1, MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException2(this, 0);
    }

    EISOPIssue363b(boolean dummy1, boolean dummy2, boolean dummy3)
            throws MyException1, @UnknownInitialization MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException1(this, 0);
    }

    EISOPIssue363b(boolean dummy1, boolean dummy2, boolean dummy3, boolean dummy4)
            throws MyException1, @UnknownInitialization MyException2 {
        throw new MyException2(this, 0);
    }

    EISOPIssue363b(String dummy) throws @UnderInitialization MyException1, MyException2 {
        throw new MyException1(this, "UnderInitialization");
    }

    EISOPIssue363b(String dummy1, String dummy2)
            throws @UnderInitialization MyException1, MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException2(this, "UnderInitialization");
    }

    EISOPIssue363b(String dummy1, String dummy2, String dummy3)
            throws MyException1, @UnderInitialization MyException2 {
        // :: error: (throw.type.invalid)
        throw new MyException1(this, "UnderInitialization");
    }

    EISOPIssue363b(String dummy1, String dummy2, String dummy3, String dummy4)
            throws MyException1, @UnderInitialization MyException2 {
        throw new MyException2(this, "UnderInitialization");
    }
}
