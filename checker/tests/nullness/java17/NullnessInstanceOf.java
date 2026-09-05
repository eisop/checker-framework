// @below-java14-jdk-skip-test
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessInstanceOf {

    public void testClassicInstanceOfNullable(Object x) {
        // :: error: (instanceof.nullable)
        if (x instanceof @Nullable String) {
            System.out.println("Nullable String instanceof check.");
        }
    }

    public void testClassicInstanceOfNonNull(Object x) {
        // :: warning: (instanceof.nonnull.redundant)
        if (x instanceof @NonNull Number) {
            System.out.println("NonNull Number instanceof check.");
        }
    }

    public void testPatternVariableNullable(Object x) {
        // :: error: (instanceof.nullable)
        if (x instanceof @Nullable String n) {
            System.out.println("Length of String: " + n.length());
        }
    }

    public void testPatternVariableNonNull(Object x) {
        // :: warning: (instanceof.nonnull.redundant)
        if (x instanceof @NonNull Number nn) {
            System.out.println("Number's hashCode: " + nn.hashCode());
        }
    }

    public void testUnannotatedClassic(Object x) {
        if (x instanceof String) {
            System.out.println("Unannotated String instanceof check.");
        }
    }

    public void testUnannotatedPatternVariable(Object x) {
        if (x instanceof String unannotatedString) {
            System.out.println("Unannotated String length: " + unannotatedString.length());
        }
    }

    public void testUnusedPatternVariable(Object x) {
        // :: error: (instanceof.nullable)
        if (x instanceof @Nullable String unusedString) {}
        // :: warning: (instanceof.nonnull.redundant)
        if (x instanceof @NonNull Number unusedNumber) {}
    }
}
