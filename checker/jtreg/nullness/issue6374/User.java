/*
 * @test
 * @summary Test case for typetools issue #6374: https://github.com/typetools/checker-framework/issues/6374
 *
 * @compile Lib.java
 * @compile/fail/ref=User.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.checker.nullness.NullnessChecker User.java
 */
// Also see checker/tests/nullness/generics/Issue6374.java
class User {
    void go() {
        Lib.decl("", null);
        // :: error: (argument.type.incompatible)
        Lib.decl((Object[]) null);
        Lib.type("", null);
        // :: error: (argument.type.incompatible)
        Lib.type((Object[]) null);
        Lib.typenn("", null);
        Lib.typenn((Object[]) null);
    }
}
