/*
 * @test
 *
 * @summary Test different defaults applied to unannotated code.
 * @compile/fail/ref=AnnotatedForWithUseNoFlag.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseConservativeDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOptimisticDefaultOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseOptimisticDefaultsForUncheckedCode=source -AonlyAnnotatedFor AnnotatedForWithUse.java
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
            // 1: OK, 2: OK, 3: Err. 4: OK
            @NonNull Object obj =  u.o;
            // 1. Err, 2: Err, TODO want OK, 3:  OK, TODO want Err, 4: Err, TODO want OK
            u.o = null;
            // 1: OK, 2: OK, 3: Err, 4: OK
            u.get().toString();
            // 1: Err, 2: Err TODO want OK, 3: Err, 4: OK
            u.set(null);
        }
    }
}
