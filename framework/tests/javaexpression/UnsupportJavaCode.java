package javaexpression;

import org.checkerframework.framework.testchecker.javaexpression.qual.FlowExp;

public class UnsupportJavaCode {

    void method() {

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("new Object()") String s0;

        // :: error: (expression.unparsable.type.invalid)
        @FlowExp("List<String> list;") String s1;
    }
}
