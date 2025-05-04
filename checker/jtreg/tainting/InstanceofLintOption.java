/*
 * @test
 * @summary Test case for instanceof lint option: -Alint=instanceof
 *
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOption.java -Alint=instanceof
 */

import org.checkerframework.checker.tainting.qual.Untainted;

// @below-java17-jdk-skip-test
public class InstanceofLintOption {
    void bar(Object o) {
        if (o instanceof @Untainted String s) {}
        if (o instanceof @Untainted String) {}
    }
}
