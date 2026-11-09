import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

// The CFG models every unboxing conversion as a call of intValue() (and the like) on the boxed
// value, so the value is non-null afterwards, as after any other dereference. Each method below
// expects the unboxing error, and no dereference error for the use that follows the unboxing.
public class UnboxingConversions {

    @Nullable String s;

    void castBoxed(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        int x = (int) i;
        i.toString();
    }

    void castBoxedWidening(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        long x = (long) i;
        i.toString();
    }

    void castOtherBoxedTypes(
            @Nullable Long l, @Nullable Double d, @Nullable Character c, @Nullable Byte b) {
        // :: error: (unboxing.of.nullable)
        long xl = (long) l;
        // :: error: (unboxing.of.nullable)
        float xd = (float) (double) d;
        // :: error: (unboxing.of.nullable)
        int xc = (int) c;
        // :: error: (unboxing.of.nullable)
        double xb = (double) b;
        l.toString();
        d.toString();
        c.toString();
        b.toString();
    }

    long returnUnboxesAndWidens(@Nullable Integer i, @Nullable Integer j, boolean c) {
        // The value of the conditional is unboxed: both operands are Integer, so its type is
        // Integer, and the return converts it to long.
        // :: error: (unboxing.of.nullable)
        return c ? i : j;
    }

    void castObject(@Nullable Object o) {
        // :: error: (unboxing.of.nullable)
        int x = (int) o;
        // The unboxing dereferences the cast `(Integer) o`, not `o`, and a cast has no
        // JavaExpression, so o is not refined.
        // :: error: (dereference.of.nullable)
        o.toString();
    }

    void castBoxes(int i) {
        Integer b = (Integer) i;
        Object o = (Object) i;
        b.toString();
        o.toString();
    }

    void conditionalAnd(@Nullable Boolean b, boolean c) {
        // :: error: (unboxing.of.nullable)
        boolean x = b && c;
        b.toString();
    }

    void conditionalOr(@Nullable Boolean b, boolean c) {
        // :: error: (unboxing.of.nullable)
        boolean x = b || c;
        b.toString();
    }

    int returnUnboxes(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        return i;
    }

    Integer returnBoxes(int i) {
        return i;
    }

    IntSupplier lambdaExpressionBody(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        return () -> i;
    }

    IntSupplier lambdaBlockBody(@Nullable Integer i) {
        return () -> {
            // :: error: (unboxing.of.nullable)
            int x = (int) i;
            // No error: i is non-null after the cast unboxed it.
            return i;
        };
    }

    Supplier<Integer> lambdaBoxes() {
        Supplier<Integer> s = () -> 5;
        return s;
    }

    LongSupplier lambdaUnboxesAndWidens(@Nullable Integer i) {
        return () -> {
            // :: error: (unboxing.of.nullable)
            long x = i;
            // No error: i is non-null after it was unboxed.
            return i;
        };
    }

    <T extends @Nullable Integer> void typeVariable(T t) {
        // :: error: (unboxing.of.nullable)
        int x = t;
        t.toString();
    }

    <T extends @Nullable Integer> void typeVariableConditional(T t, boolean c) {
        // Both branches unbox t, so t is non-null after the conditional.
        // :: error: (unboxing.of.nullable)
        int x = c ? t : t;
        t.toString();
    }

    // A conditional postcondition on a method that returns Boolean. The method boxes its returned
    // condition, and a caller unboxes the result; both conversions keep the then and else stores.

    @EnsuresNonNullIf(expression = "s", result = true)
    Boolean isSet() {
        return s != null;
    }

    void useIsSet() {
        if (isSet()) {
            s.toString();
        }
    }

    @EnsuresNonNullIf(expression = "s", result = false)
    Boolean isUnset() {
        if (s == null) {
            // Returns true, so the postcondition for false does not apply here.
            return true;
        }
        return false;
    }

    @EnsuresNonNullIf(expression = "s", result = true)
    boolean isSetOpaque(Boolean b) {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return b;
    }
}
