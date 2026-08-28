/*
 * @test
 * @summary A stub file for a class that is being compiled is used only with
 * -AmergeStubsWithSource.  Without that flag an AnnotatedFor annotation written in the stub does
 * not opt the source class into type-checking, so the class stays unchecked and its callers get
 * conservative defaults.
 *
 * @compile/fail/ref=NoStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source Lib.java Use.java
 * @compile/fail/ref=NoStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -Astubs=Lib.astub Lib.java Use.java
 * @compile/fail/ref=MergeStubs.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -AuseConservativeDefaultsForUncheckedCode=source -AmergeStubsWithSource -Astubs=Lib.astub Lib.java Use.java
 */

package annotatedforconservativedefaults;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class Use {
    void f(Lib lib) {
        @NonNull Object o = lib.get();
    }
}
