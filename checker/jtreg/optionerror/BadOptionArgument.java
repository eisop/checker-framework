/*
 * @test
 *
 * @summary Test that a bad argument to an option that SourceChecker.init reads is reported as a
 * compiler error, and that the checker then stops.  init is called by javac itself, so an
 * exception thrown out of it is reported as an uncaught processor exception with a stack trace
 * instead.  The type error below is reported only when the option is well-formed, which shows
 * that a bad one stops the checker rather than being ignored.
 *
 * @compile/fail/ref=MissingArgument.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AwarnUnneededSuppressionsExceptions BadOptionArgument.java
 * @compile/fail/ref=BadRegex.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AwarnUnneededSuppressionsExceptions=[ BadOptionArgument.java
 * @compile/fail/ref=Checked.out -XDrawDiagnostics -processor org.checkerframework.checker.tainting.TaintingChecker -AwarnUnneededSuppressionsExceptions=nosuchkey BadOptionArgument.java
 */

import org.checkerframework.checker.tainting.qual.Untainted;

public class BadOptionArgument {
    static String tainted() {
        return "tainted";
    }

    @Untainted String f = tainted();
}
