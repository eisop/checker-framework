// Test case for EISOP issue #2084:
// https://github.com/eisop/checker-framework/issues/2084
// A variant of java8inference/Issue2084.java in which the generic method is invoked on the
// implicitly typed lambda parameter in the arms of a switch expression.

// @below-java14-jdk-skip-test

import java.util.List;
import java.util.function.Function;

public class Issue2084Switch {

    static <A, R> R of(A a, Function<A, R> f) {
        throw new Error();
    }

    String[] switchBody(List<String> list, int i) {
        return of(
                list,
                l ->
                        switch (i) {
                            case 0 -> l.toArray(new String[0]);
                            default -> i > 1 ? l.toArray(new String[0]) : l.toArray(new String[0]);
                        });
    }

    Object switchWithYield(List<String> list, int i) {
        return of(
                list,
                l ->
                        switch (i) {
                            case 0 -> l.size();
                            case 1 -> (l.toArray(new String[0]));
                            default -> {
                                if (i > 5) {
                                    yield l.get(0);
                                }
                                yield l.toArray(new String[0]);
                            }
                        });
    }
}
