import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;

// The error key for the unboxing, or the iteration, of a possibly-null value depends on its
// context. Each method has one error, because dataflow refines the value to non-null after it.
public class UnboxingContexts {
    void ifCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        if (b) {}
    }

    void whileCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        while (b) {}
    }

    void doWhileCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        do {} while (b);
    }

    void forCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        for (; b; ) {}
    }

    void conditionalCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        int i = b ? 1 : 2;
    }

    void parenthesizedCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        if ((b)) {}
    }

    void andOperand(@Nullable Boolean b, boolean c) {
        // :: error: (unboxing.of.nullable)
        if (b && c) {}
    }

    void notOperand(@Nullable Boolean b) {
        // :: error: (unboxing.of.nullable)
        if (!b) {}
    }

    void assertCondition(@Nullable Boolean b) {
        // :: error: (condition.nullable)
        assert b;
    }

    void switchBoxed(@Nullable Integer i) {
        // :: error: (switching.nullable)
        switch (i) {
            default:
        }
    }

    void switchString(@Nullable String s) {
        // :: error: (switching.nullable)
        switch (s) {
            default:
        }
    }

    void assignment(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = i;
    }

    void binary(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = i + 1;
    }

    void unary(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = -i;
    }

    void increment(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        i++;
    }

    void compoundVariable(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        i += 1;
    }

    void cast(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = (int) i;
    }

    void castObject(@Nullable Object o) {
        // :: error: (unboxing.of.nullable)
        int j = (int) o;
    }

    void index(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = a[i];
    }

    void dimension(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int[] b = new int[i];
    }

    void conditionalBranch(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        int j = c ? i : 0;
    }

    void conditionalBooleanBranch(@Nullable Boolean b, boolean c) {
        // :: error: (unboxing.of.nullable)
        boolean d = c ? b : false;
    }

    void equalsPrimitive(@Nullable Integer i, int[] a, boolean c) {
        // :: error: (unboxing.of.nullable)
        boolean e = i == 0;
    }

    void compoundValue(@Nullable Integer i) {
        int j = 0;
        // :: error: (unboxing.of.nullable)
        j += i;
    }

    int returned(@Nullable Integer i) {
        // :: error: (unboxing.of.nullable)
        return i;
    }

    void stringConcatenation(@Nullable Integer i) {
        String s = "" + i;
        s += i;
    }

    void referenceEquality(@Nullable Integer i, @Nullable Integer j) {
        boolean e = i == j;
    }

    void iterableNullable(@Nullable List<Integer> list) {
        // :: error: (iterating.over.nullable)
        for (Integer x : list) {}
    }

    void arrayNullable(Integer @Nullable [] array) {
        // :: error: (iterating.over.nullable)
        for (Integer x : array) {}
    }

    void iterableElement(List<@Nullable Integer> list) {
        // :: error: (unboxing.of.nullable)
        for (int x : list) {}
    }

    void iterableElementWidened(List<@Nullable Integer> list) {
        // :: error: (unboxing.of.nullable)
        for (long x : list) {}
    }

    void arrayElement(@Nullable Integer[] array) {
        // :: error: (unboxing.of.nullable)
        for (int x : array) {}
    }

    void boxedElements(List<@Nullable Integer> list, @Nullable Integer[] array) {
        for (Integer x : list) {}
        for (Integer x : array) {}
    }

    void nonNullElements(List<Integer> list, Integer[] array) {
        for (int x : list) {}
        for (long x : list) {}
        for (int x : array) {}
    }

    void explicitIteratorWhile(List<@Nullable Integer> list) {
        Iterator<@Nullable Integer> it = list.iterator();
        while (it.hasNext()) {
            // :: error: (unboxing.of.nullable)
            int x = it.next();
        }
    }

    void explicitIteratorFor(List<@Nullable Integer> list) {
        for (Iterator<@Nullable Integer> it = list.iterator(); it.hasNext(); ) {
            // :: error: (unboxing.of.nullable)
            long x = it.next();
        }
    }

    void explicitIteratorDereference(List<@Nullable Integer> list) {
        for (Iterator<@Nullable Integer> it = list.iterator(); it.hasNext(); ) {
            // :: error: (dereference.of.nullable)
            it.next().toString();
            @Nullable Integer y = it.next();
        }
    }

    void explicitIteratorNonNull(List<Integer> list) {
        for (Iterator<Integer> it = list.iterator(); it.hasNext(); ) {
            int x = it.next();
        }
    }

    void explicitIteratorNullable(@Nullable Iterator<Integer> it) {
        // :: error: (dereference.of.nullable)
        while (it.hasNext()) {}
    }

    void explicitIteratorCall(@Nullable List<Integer> list) {
        // :: error: (dereference.of.nullable)
        Iterator<Integer> it = list.iterator();
    }
}
