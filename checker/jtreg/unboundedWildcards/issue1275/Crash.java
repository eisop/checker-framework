/*
 * @test
 * @summary Test case for Issue 1275.
 * https://github.com/typetools/checker-framework/issues/1275
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Lib.java
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext Crash.java
 */
public class Crash {
    void crash(Sub o) {
        Sub.SubInner<?> x = o.a().b().b();
        o.a().b().b().c();
    }
}
