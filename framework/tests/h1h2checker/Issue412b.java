import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

public class Issue412b {

    // :: warning: (inconsistent.constructor.type) :: error: (super.invocation.invalid)
    @H1Bot Issue412b() {
        new Inner();
    }

    class Inner {
        /* The framework can correctly identify the outer class receiver type to be @H1Top.
        This rules out the case that this issue is caused by the general framework. */
        Inner(@H1Top Issue412b Issue412b.this) {

            // :: error: (assignment.type.incompatible)
            @H1Bot Issue412b object = Issue412b.this;
        }
    }
}
