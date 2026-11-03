/*
 * @test
 *
 * @summary Test that an exception thrown while computing the type of the left-hand side of an
 * assignment does not disable flow-sensitive refinement for the compilation units that follow.
 * LhsException.java makes the checker crash; LhsExceptionNext.java, checked after it in the same
 * javac run, needs flow-sensitive refinement to type-check.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsExceptionNext.java
 * @compile/fail/ref=LhsException.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsException.java LhsExceptionNext.java
 */

public class LhsException {
    void crash() {
        int throwInLhs = 0;
    }
}
