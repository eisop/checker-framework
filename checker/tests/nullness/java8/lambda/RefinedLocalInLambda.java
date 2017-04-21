// Test case for issue #1248:
// https://github.com/typetools/checker-framework/issues/1248
// @skip-test until the issue is fixed

import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RefinedLocalInLambda {

    public static void main(String[] args) {
        printIntegersGreaterThan(10);
    }

    public static void printIntegersGreaterThan(@Nullable Integer limit) {
        if (limit == null) {
            return;
        }
        printIntegersWithPredicate(i -> i > limit);
    }

    public static void printIntegersWithPredicate(Predicate<Integer> tester) {
        for (int i = 0; i < 100; i++) {
            if (tester.test(i)) {
                System.out.println(i);
            }
        }
    }
}
