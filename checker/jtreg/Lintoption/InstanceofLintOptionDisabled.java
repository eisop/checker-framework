/*
 * @test
 * @summary Test case for instanceof lint option: -Alint=-instanceof
 * @requires jdk.version >= 17
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionEnabled.java -Alint=-instanceof
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceofLintOptionDisabled {
    void bar(Object o) {
        if (o instanceof @Untainted String s) {}
        if (o instanceof @Untainted String) {}
    }
}
