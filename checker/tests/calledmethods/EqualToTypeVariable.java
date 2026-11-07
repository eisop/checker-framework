import org.checkerframework.checker.calledmethods.qual.CalledMethods;

// Refining an operand of == by the other operand's value, where one of them is a use of a type
// variable without a primary annotation.
public class EqualToTypeVariable {
    void requireClose(@CalledMethods("close") Object o) {}

    <T> void sameTypeVariable(@CalledMethods("toString") T a, T b) {
        if (a == b) {
            // Only toString has been called on the object.
            // :: error: (argument.type.incompatible)
            requireClose(b);
        }
    }
}
