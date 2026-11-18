/*
 * @test
 *
 * @summary Test that an exception thrown while computing the type of the left-hand side of an
 * assignment does not change how the compilation units that follow are checked.
 * LhsException.java makes the checker crash; LhsExceptionNext.java, checked after it in the same
 * javac run, needs flow-sensitive refinement to type-check. Likewise, LhsExceptionTypeVar.java
 * makes the checker crash while computing the assignment context for type argument inference, and
 * LhsExceptionTypeVarNext.java needs the local variable default for a type variable to type-check.
 *
 * @compile -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsExceptionNext.java
 * @compile/fail/ref=LhsException.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsException.java LhsExceptionNext.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsExceptionTypeVarNext.java
 * @compile/fail/ref=LhsExceptionTypeVar.out -XDrawDiagnostics -processor org.checkerframework.framework.testchecker.lhsexception.LhsExceptionChecker LhsExceptionTypeVar.java LhsExceptionTypeVarNext.java
 */

public class LhsException {
    void crash() {
        int throwInLhs = 0;
    }
}
