/*
 * @test
 * @summary Test that record stubs are ignored for source records unless -AmergeStubsWithSource is passed
 *
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Werror Use.java Rec.java
 * @compile/ref=WithStub.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=Rec.astub Use.java Rec.java
 * @compile/fail/ref=WithStubAndMerge.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=Rec.astub -AmergeStubsWithSource Use.java Rec.java
 */

package records;

import org.checkerframework.checker.nullness.qual.NonNull;

public class Use {
    void g(Rec r) {
        @NonNull Object o = r.f();
    }
}
