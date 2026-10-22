import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import java.util.ArrayList;
import java.util.Arrays;

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
}
