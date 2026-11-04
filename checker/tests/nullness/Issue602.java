import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Test case for Issue 602
// https://github.com/typetools/checker-framework/issues/602
// The type of an expression that depends on the refined types of its subexpressions must not be
// cached while the dataflow analysis is still iterating over the loop.
public class Issue602 {
    @PolyNull String id(@PolyNull String o) {
        return o;
    }

    <T extends @Nullable Object> T gid(T o) {
        return o;
    }

    void loop(boolean condition) {
        @NonNull String notNull = "hello";
        String nullable = "";
        while (condition) {
            // :: error: (assignment.type.incompatible)
            notNull = nullable;
            // :: error: (assignment.type.incompatible)
            notNull = id(nullable);
            // :: error: (dereference.of.nullable)
            gid(nullable).length();
            // :: error: (assignment.type.incompatible)
            notNull = condition ? nullable : "x";
            // :: error: (assignment.type.incompatible)
            notNull = new ArrayList<>(Arrays.asList(nullable)).get(0);
            nullable = null;
        }
    }

    static <T> T bar() {
        throw new Error();
    }

    static <T extends @Nullable Object> @PolyNull Object first(@PolyNull T a, T b) {
        return a;
    }

    static @PolyNull Object pid(@PolyNull Object o) {
        return o;
    }

    // Inferring the type arguments of bar() also types the later argument pid(x), whose node has
    // not been visited yet in the current pass over the loop.
    void inferredWithLaterArgument(@Nullable Object y, boolean condition) {
        Object x = "";
        while (condition) {
            // :: error: (dereference.of.nullable)
            first(bar(), pid(x)).toString();
            x = y;
        }
    }

    static <T> List<T> listBar() {
        throw new Error();
    }

    static <T> void foo(List<T> a, T b) {}

    void noFalsePositiveWithLaterArgument(@Nullable Object y, boolean condition) {
        Object x = "";
        while (condition) {
            foo(listBar(), pid(x));
            x = y;
        }
    }
}
