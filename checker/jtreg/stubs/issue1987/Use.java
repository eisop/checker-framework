/*
 * @test
 * @summary Test case for Issue 1987 https://github.com/eisop/checker-framework/issues/1987
 *
 * @compile/fail/ref=WithoutStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source Use.java Lib.java
 * @compile/fail/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -Astubs=Lib.astub Use.java Lib.java
 * @compile/fail/ref=WithStubAndMerge.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -Astubs=Lib.astub -AmergeStubsWithSource Use.java Lib.java
 */

package issue1987;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class Use {
    void f(Lib lib) {
        @NonNull Object o = lib.get();
    }
}
