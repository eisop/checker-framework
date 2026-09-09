/*
 * @test
 *
 * @summary Test that -Amode rejects a mode the checker does not support, and requires a value.
 *
 * @compile/fail/ref=UnsupportedMode.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=nosuchmode UnsupportedMode.java
 * @compile/fail/ref=EmptyMode.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Amode= UnsupportedMode.java
 */

public class UnsupportedMode {}
