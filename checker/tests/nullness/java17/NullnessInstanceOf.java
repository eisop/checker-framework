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

    // In "@Nullable String[]", the annotation is on the array's component type, not on the array
    // itself.  Without a pattern, no variable is bound, so the annotation constrains nothing.
    public void testComponentWithoutPattern(Object x) {
        // :: error: (instanceof.component)
        if (x instanceof @Nullable String[]) {}
        // :: error: (instanceof.component)
        if (x instanceof @NonNull String[]) {}
    }

    // With a pattern, the same component annotation does constrain something: the elements of the
    // bound variable.  It is not reported, and "a[0]" is possibly-null below.
    public void testComponentWithPattern(Object x) {
        if (x instanceof @Nullable String[] a) {
            // :: error: (dereference.of.nullable)
            System.out.println(a[0].length());
        }
    }

    // The array's own root annotation is written after the component type.  Unlike the component
    // annotation, it is about the tested reference itself.
    public void testArrayRoot(Object x) {
        // :: error: (instanceof.nullable)
        if (x instanceof String @Nullable []) {}
        // :: error: (instanceof.nullable)
        if (x instanceof String @Nullable [] a) {}
        // :: warning: (instanceof.nonnull.redundant)
        if (x instanceof String @NonNull [] b) {}
    }
}
