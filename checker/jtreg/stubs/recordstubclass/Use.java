/*
 * @test
 * @summary A record in a stub file whose library type is a class must not stop the processing of
 *     the members that follow it.
 *
 * @compile Outer.java
 * @compile/fail/ref=Use.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=Outer.astub Use.java
 */

package recordstubclass;

import org.checkerframework.checker.nullness.qual.NonNull;

public class Use {
    void g(Outer o) {
        @NonNull Object x = o.get();
    }
}
