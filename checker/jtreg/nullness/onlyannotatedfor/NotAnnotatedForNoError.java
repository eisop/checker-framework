/*
 * @test
 *
 * @summary Test that command-line option -AonlyAnnotatedFor suppresses warnings for code that is not annotated for the corresponding checker.
 * @compile/fail/ref=NotAnnotatedForNoError.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor NotAnnotatedForNoError.java
 *
 */

import org.checkerframework.framework.qual.AnnotatedFor;

public class NotAnnotatedForNoError {
    @AnnotatedFor("nullness")
    class A {
        // :: error: (assignment.type.incompatible)
        Object o = null;
    }

    @AnnotatedFor("regex")
    class B {
        // No expected error, because code is not annotated for nullness.
        Object o = null;
    }

    class C {
        // No expected error, because code is not annotated for nullness.
        Object o = null;
    }
}
