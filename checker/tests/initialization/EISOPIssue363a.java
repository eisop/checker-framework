// Test case for issue 363:
// https://github.com/eisop/checker-framework/issues/363

import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;

public class EISOPIssue363a {
    class MyException extends Exception {
        @NotOnlyInitialized EISOPIssue363a cause;

        MyException(EISOPIssue363a cause) {
            this.cause = cause;
        }

        MyException(@UnderInitialization EISOPIssue363a cause, String dummy) {
            this.cause = cause;
        }

        MyException(@UnknownInitialization EISOPIssue363a cause, int dummy) {
            this.cause = cause;
        }
    }

    Object field;

    EISOPIssue363a() throws MyException {
        // :: error: (throw.type.incompatible)
        // :: error: (argument.type.incompatible)
        throw new MyException(this);
    }

    EISOPIssue363a(boolean dummy1, boolean dummy2) throws MyException {
        // :: error: (throw.type.incompatible)
        throw new MyException(this, 0);
    }

    EISOPIssue363a(boolean dummy1, boolean dummy2, boolean dummy3) throws MyException {
        // :: error: (throw.type.incompatible)
        throw new MyException(this, "UnderInitialization");
    }

    EISOPIssue363a(int dummy) throws @UnknownInitialization MyException {
        throw new MyException(this, 0);
    }

    EISOPIssue363a(int dummy1, int dummy2) throws @UnknownInitialization MyException {
        throw new MyException(this, "UnderInitialization");
    }

    EISOPIssue363a(int dummy1, int dummy2, int dummy3) throws @UnknownInitialization MyException {
        // :: error: (argument.type.incompatible)
        throw new MyException(this);
    }

    EISOPIssue363a(String dummy) throws @UnderInitialization MyException {
        throw new MyException(this, "UnderInitialization");
    }

    EISOPIssue363a(String dummy1, String dummy2) throws @UnderInitialization MyException {
        throw new MyException(this, 0);
    }

    EISOPIssue363a(String dummy1, String dummy2, String dummy3)
            throws @UnderInitialization MyException {
        // :: error: (argument.type.incompatible)
        throw new MyException(this);
    }

    void test1() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a(1);
        } catch (MyException ex) {
            ex.cause.field.toString();
        }
    }

    void test2() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a();
        } catch (@UnknownInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    void test3() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a();
        } catch (@UnderInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    void test4() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a(1);
        } catch (MyException ex) {
            ex.cause.field.toString();
        }
    }

    void test5() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a(1);
        } catch (@UnknownInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    void test6() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a(1);
        } catch (@UnderInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    void test7() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a("UnderInitialization");
        } catch (MyException ex) {
            ex.cause.field.toString();
        }
    }

    void test8() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a("UnderInitialization");
        } catch (@UnknownInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    void test9() {
        try {
            EISOPIssue363a obj = new EISOPIssue363a("UnderInitialization");
        } catch (@UnderInitialization MyException ex) {
            // :: error: (dereference.of.nullable)
            ex.cause.field.toString();
        }
    }

    <X extends Throwable> void throwIfInstanceOf(Throwable throwable, Class<X> declaredType)
            throws X {
        if (declaredType.isInstance(throwable)) {
            throw declaredType.cast(throwable);
        }
    }
}
