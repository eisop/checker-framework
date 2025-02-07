/*
 * @test
 * @summary Test case for EISOP issue #673: https://github.com/eisop/checker-framework/issues/673
 *
 * `Lib7.class` is the result of compiling `Lib7.source` (with a `.java` extension) with a Java 7
 * compiler (or with  `--release 7` for Java <= 19 compiler).
 *
 * @compile Lib.java
 * @compile/fail/ref=User.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker User.java
 */
class User {
    void go(Lib lib, Lib7 lib7) {
        String[] b = lib.get();
        String[] b7 = lib7.get();
    }
}
