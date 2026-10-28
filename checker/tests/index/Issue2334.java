// Test case for issue #2334: https://tinyurl.com/cfissue/2334
//
// The refinement of a method call is evicted by any call to a method with side effects, even if
// the receiver and the arguments of the call cannot be modified, because the value of a call may
// depend on the heap (see https://github.com/eisop/checker-framework/issues/2326). So the
// refinement of stringFormal.indexOf('d') does not survive hasSideEffect() or println either.

import org.checkerframework.checker.index.qual.NonNegative;

public class Issue2334 {

    void hasSideEffect() {}

    String stringField;

    void m1(String stringFormal) {
        if (stringFormal.indexOf('d') != -1) {
            hasSideEffect();
            // :: error: (assignment.type.incompatible)
            @NonNegative int i = stringFormal.indexOf('d');
        }
    }

    void m2() {
        if (stringField.indexOf('d') != -1) {
            hasSideEffect();
            // :: error: (assignment.type.incompatible)
            @NonNegative int i = stringField.indexOf('d');
        }
    }

    void m3(String stringFormal) {
        if (stringFormal.indexOf('d') != -1) {
            System.out.println("hey");
            // :: error: (assignment.type.incompatible)
            @NonNegative int i = stringFormal.indexOf('d');
        }
    }

    void m4() {
        if (stringField.indexOf('d') != -1) {
            System.out.println("hey");
            // :: error: (assignment.type.incompatible)
            @NonNegative int i = stringField.indexOf('d');
        }
    }
}
