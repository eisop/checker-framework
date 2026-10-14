/*
 * @test
 * @summary Test NullnessNoInitSubchecker standalone mode/lint/skipDefs support.
 *
 * @compile/fail/ref=NullnessNoInitStandalone.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker NullnessNoInitStandalone.java
 * @compile/fail/ref=NullnessNoInitStandaloneSkipDefs.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker -AskipDefs=SkipMe NullnessNoInitStandalone.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker -Amode=jspecify NullnessNoInitStandalone.java
 * @compile/fail/ref=NullnessNoInitStandalone.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker -Alint=trustArrayLenZero NullnessNoInitStandalone.java
 */

public class NullnessNoInitStandalone {

    static class SkipMe {
        static Object foo() {
            return null;
        }
    }

    static class DontSkip {
        static Object foo() {
            return null;
        }
    }
}
