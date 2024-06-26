/*
 * @test
 * @summary Test case for EISOP issue #608.
 *
 * @compile/fail/ref=EisopIssue608-nostub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Werror EisopIssue608.java
 * @compile/fail/ref=EisopIssue608-badstub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Werror -Astubs=EisopIssue608-bad.astub EisopIssue608.java
 * @compile -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Werror -AstubNoWarnIfNotFound -Astubs=collection-object-parameters-may-be-null.astub EisopIssue608.java
 */
import java.util.List;

public class EisopIssue608 {
    boolean go(List<String> l) {
        return l.contains(null);
    }
}
