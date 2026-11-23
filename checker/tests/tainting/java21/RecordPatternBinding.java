// @below-java21-jdk-skip-test

// The variable of a type pattern nested in a record pattern has the qualifier of the record
// component that the pattern is matched against, not the qualifier of the instanceof operand.
// The variable of a top-level type pattern has the qualifier of the operand.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class RecordPatternBinding {

    record Box(Object contents) {}

    record UBox(@Untainted Object contents) {}

    record G<T>(T t) {}

    void taintedComponent(@Untainted Object o) {
        if (o instanceof Box(Object c)) {
            // :: error: (assignment.type.incompatible)
            @Untainted Object u = c;
        }
        if (o instanceof Box b) {
            @Untainted Object u = b;
        }
    }

    void untaintedComponent(@Tainted Object o) {
        if (o instanceof UBox(Object c)) {
            @Untainted Object u = c;
        }
        // A nested pattern that is not unconditional for the component also has the component's
        // qualifier.
        if (o instanceof UBox(String s)) {
            @Untainted String u = s;
        }
        if (o instanceof UBox(var v)) {
            @Untainted Object u = v;
        }
    }

    void nestedRecordPattern(@Tainted Object o) {
        if (o instanceof Box(UBox(Object c))) {
            @Untainted Object u = c;
        }
        if (o instanceof UBox(Box(Object c))) {
            // :: error: (assignment.type.incompatible)
            @Untainted Object u = c;
        }
    }

    void inferredTypeArguments(G<@Untainted String> untainted, @Untainted G<String> tainted) {
        if (untainted instanceof G(String s)) {
            @Untainted String u = s;
        }
        if (tainted instanceof G(String s)) {
            // :: error: (assignment.type.incompatible)
            @Untainted String u = s;
        }
    }

    void inferredFromWildcard(
            G<? extends @Untainted String> untainted, G<? extends @Tainted String> tainted) {
        if (untainted instanceof G(String s)) {
            @Untainted String u = s;
        }
        if (untainted instanceof G(var v)) {
            @Untainted String u = v;
        }
        if (tainted instanceof G(String s)) {
            // :: error: (assignment.type.incompatible)
            @Untainted String u = s;
        }
    }

    <U extends G<@Untainted String>> void inferredFromTypeVariable(U untainted) {
        if (untainted instanceof G(String s)) {
            @Untainted String u = s;
        }
    }

    void negated(@Tainted Object o) {
        if (!(o instanceof UBox(Object c))) {
            return;
        }
        @Untainted Object u = c;
    }
}
