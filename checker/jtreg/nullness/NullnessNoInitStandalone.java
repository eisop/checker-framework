/*
 * @test
 *
 * @summary Test that NullnessNoInitSubchecker can run standalone as an annotation processor
 * without throwing NullPointerException in shouldSkipDefs or Illegal option errors.
 *
 * @compile/fail/ref=NullnessNoInitStandalone.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker NullnessNoInitStandalone.java
 * @compile/fail/ref=NullnessNoInitStandaloneSkipDefs.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessNoInitSubchecker -AskipDefs=SkipMe NullnessNoInitStandalone.java
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
