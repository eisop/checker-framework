// Test case for issue 363:
// https://github.com/eisop/checker-framework/issues/363

import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

class MyException extends Exception {
    @NotOnlyInitialized EISOPIssue363a cause;

    MyException(EISOPIssue363a cause) {
        this.cause = cause;
    }

    MyException(@Initialized EISOPIssue363a cause, String dummy) {
        this.cause = cause;
    }

    MyException(@UnknownInitialization EISOPIssue363a cause, int dummy) {
        this.cause = cause;
    }
}

public class EISOPIssue363a {
    Object field;

    EISOPIssue363a() throws MyException {
        // :: error: (throw.type.incompatible)
        // :: error: (argument.type.incompatible)
        throw new MyException(this);
    }

    EISOPIssue363a(boolean dummy) throws MyException {
        // :: error: (throw.type.incompatible)
        throw new MyException(this, 0);
    }

    EISOPIssue363a(char dummy) throws MyException {
        // :: error: (throw.type.incompatible)
        // :: error: (argument.type.incompatible)
        throw new MyException(this, "a");
    }

    EISOPIssue363a(String dummy) throws @UnknownInitialization MyException {
        // :: error: (argument.type.incompatible)
        throw new MyException(this);
    }

    EISOPIssue363a(int dummy) throws @UnknownInitialization MyException {
        throw new MyException(this, 0);
    }

    public static void test1() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a();
        } catch (MyException ex) {
            ex.cause.field.toString();
        }
    }

    public static void test2(String[] args) {
        try {
            EISOPIssue363a obj = new EISOPIssue363a();
        } catch (@UnknownInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    public static void test3(String[] args) {
        try {
            EISOPIssue363a obj = new EISOPIssue363a();
        } catch (@Initialized MyException ex) {
            ex.cause.field.toString();
        }
    }

    public static void test4(String[] args) {
        try {
            EISOPIssue363a obj = new EISOPIssue363a(0);
        } catch (MyException ex) {
            ex.cause.field.toString();
        }
    }
}
