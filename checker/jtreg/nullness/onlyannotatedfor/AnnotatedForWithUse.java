/*
 * @test
 *
 * @summary Test different defaults applied to unannotated code.
 * @compile/fail/ref=AnnotatedForWithUseNoFlag.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseOnlyAnnotatedFor.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseJSpecifyMode.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Amode=jspecify AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUseConservativeDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source AnnotatedForWithUse.java
 * @compile/fail/ref=AnnotatedForWithUsePermissiveDefault.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -AusePermissiveDefaultsForUncheckedCode=source AnnotatedForWithUse.java
 */
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

public class AnnotatedForWithUse {
    class Unannotated {
        Object o;

        Object get() {
            return null;
        }

        void set(Object of) {}
    }

    @AnnotatedFor("nullness")
    class AnnotatedUse {
        void use(Unannotated u) {
            // 1: OK, 2: OK, 3: OK, 4: Err, 5: OK
            @NonNull Object obj = u.o;
            // 1: Err, 2: Err, 3: Err, 4: OK (unsound), 5: Err
            // Case 4 (conservative defaults) is unsound: protects reads, not writes.
            // Case 5 (permissive defaults) defaults field write to NonNull.
            // See https://github.com/eisop/checker-framework/issues/1358 .
            u.o = null;
            // 1: OK, 2: OK, 3: OK, 4: Err, 5: OK
            u.get().toString();
            // 1: Err, 2: Err, 3: Err, 4: Err, 5: OK
            u.set(null);
        }
    }
}
