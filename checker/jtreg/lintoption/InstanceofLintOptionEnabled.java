/*
 * @test
 * Test case for instanceof lint option: -Alint=instanceof
 * @requires jdk.version >= 17
 * @compile/ref=InstanceofLintOptionEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionEnabled.java
 * @compile/ref=InstanceofLintOptionEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionEnabled.java -Alint=instanceof
 * @compile/ref=InstanceofLintOptionEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionEnabled.java -Alint=instanceof:unsafe
 * @compile/ref=InstanceofLintOptionEnabled.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker InstanceofLintOptionEnabled.java -Alint=-instanceof,instanceof:unsafe
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class InstanceofLintOptionEnabled {
    void bar(Object o) {
        if (o instanceof @Untainted String s) {}
        if (o instanceof @Untainted String) {}
    }
}
