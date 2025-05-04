/*
 * @test
 * @summary Test case that issuess warnings for unsafe instanceof patterns
 * @requires jdk.version >= 17
 * @compile -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionDisabled.java
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceofLintOptionDisabled {
    void bar(Object o) {
        if (o instanceof @Untainted String s) {}
        if (o instanceof @Untainted String) {}
    }
}
