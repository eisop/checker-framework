// Test case for issue 363:
// https://github.com/eisop/checker-framework/issues/363

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class MyException1 extends Exception {
    @NotOnlyInitialized EISOPIssue363b cause;

    MyException1(EISOPIssue363b cause) {
        this.cause = cause;
    }

    MyException1(@Initialized EISOPIssue363b cause, String dummy) {
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

    MyException2(@Initialized EISOPIssue363b cause, String dummy) {
        this.cause = cause;
    }

    MyException2(@UnknownInitialization EISOPIssue363b cause, int dummy) {
        this.cause = cause;
    }
}

public class EISOPIssue363b {
    Object field;

    EISOPIssue363b() throws @Initialized MyException1, @Initialized MyException2{
        // :: error: (throw.type.incompatible)
        throw new MyException1(this, 0);
    }

    EISOPIssue363b(int dummy) throws @Initialized MyException1, @Initialized MyException2{
        // :: error: (throw.type.incompatible)
        throw new MyException2(this, 0);
    }

    EISOPIssue363b(boolean dummy) throws @UnknowInitialization MyException1, @Initialized MyException2{
        // :: error: (throw.type.incompatible)
        throw new MyException2(this, 0);
    }

    EISOPIssue363b(float dummy) throws @Initialized MyException1, @UnknowInitialization MyException2{
        throw new MyException2(this, 0);
    }
}
