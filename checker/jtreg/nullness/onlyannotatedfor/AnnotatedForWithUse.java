/*
 * @test
 *
 * @summary Test that command-line option -AonlyAnnotatedFor suppresses warnings for code that is not annotated for the corresponding checker.
 * @compile/fail/ref=OnlyAnnotatedForWithUse.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor AnnotatedForWithUse.java
 * @compile/fail/ref=ConservativeDefaultWithUse.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source AnnotatedForWithUse.java
 */
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

public class AnnotatedForWithUse {
    class Unannotated {
        Object o;
        Object get() { return null; }
        void set(Object of) {}
    }

    @AnnotatedFor("nullness")
    class AnnotatedUse {
        void use(Unannotated u) {
            @NonNull Object obj =  u.o;
            u.o = null;
            u.get().toString();
            u.set(null);
        }
    }
}
