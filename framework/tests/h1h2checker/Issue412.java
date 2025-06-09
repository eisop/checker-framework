import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// @skip-test

public class Issue412 {

    @H1Bot Issue412() {
        new Inner();
    }

    class Inner {
        /* The framework can correctly identify the outer class reciever type to be @H1Top.
        This rules out the case that this issue is caused by the general framework. */
        // :: warning : (inconsistent.constructor.type) :: error : (super.invocation.invalid)
        Inner(@H1Top Issue412 Issue412.this) {

            // :: error : (assignment.type.incompatible)
            @H1Bot Issue412 object = Issue412.this;
        }
    }

    public static void main(String[] args) {
        new Issue412();
    }
}
