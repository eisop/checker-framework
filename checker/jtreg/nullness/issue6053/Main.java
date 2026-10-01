/*
 * @test
 * @summary Test case for typetools issue #6053: https://github.com/typetools/checker-framework/issues/6053
 *
 * @compile/ref=Main.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=stubs.jar Main.java
 */
public class Main {
    public static void main(String[] args) {}
}
