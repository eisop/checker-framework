/*
 * @test
 * @summary Test SourceChecker.getErrorCount: a suppressed error is not counted, and an error that
 * -Awarns reports as a warning is.
 *
 * @compile/fail/ref=ErrorCount.out -XDrawDiagnostics -Anomsgtext -processor org.checkerframework.framework.testchecker.errorcount.ErrorCountChecker ErrorCount.java
 * @compile/ref=ErrorCountWarns.out -XDrawDiagnostics -Anomsgtext -Awarns -processor org.checkerframework.framework.testchecker.errorcount.ErrorCountChecker ErrorCount.java
 */

public class ErrorCount {
    class ReportOnMe1 {}

    class ReportOnMe2 {}

    @SuppressWarnings("errorcount")
    class ReportOnMe3 {}
}
