package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class Fields {
    static class String {
        public static final java.lang.String HELLO = "hello";
    }

    void method(
            // :: error: (expression.unparsable.type.invalid)
            @FlowExp("java.lang.String.HELLO") Object p1,
            @FlowExp("Fields.String.HELLO") Object p2) {
        // :: error: (assignment.type.incompatible)
        @FlowExp("String.HELLO") Object l1 = p1;
        @FlowExp("String.HELLO") Object l2 = p2;
        @FlowExp("javaexpression.Fields.String.HELLO") Object l3 = p2;
    }
}
