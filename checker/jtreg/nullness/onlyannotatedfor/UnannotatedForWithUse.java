/*
 * @test
 *
 * @summary Test the defaults applied to code that UnannotatedFor excludes from an enclosing
 * AnnotatedFor scope, as seen from checked code that uses it.
 * @compile/fail/ref=UnannotatedForWithUseNoFlag.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker UnannotatedForWithUse.java
 * @compile/fail/ref=UnannotatedForWithUseOnlyAnnotatedFor.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AonlyAnnotatedFor UnannotatedForWithUse.java
 * @compile/fail/ref=UnannotatedForWithUseConservativeDefault.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source UnannotatedForWithUse.java
 */
import org.checkerframework.framework.qual.AnnotatedFor;
import org.checkerframework.framework.qual.UnannotatedFor;

@AnnotatedFor("nullness")
public class UnannotatedForWithUse {

    // Excluded from the enclosing AnnotatedFor scope, so its unannotated signature is defaulted
    // as unchecked code: conservatively under -AuseConservativeDefaultsForUncheckedCode=source,
    // with the ordinary CLIMB defaults otherwise.  This is the effect of UnannotatedFor that is
    // invisible from inside the excluded scope itself.  Numbered comments in use() below refer to
    // the three @compile runs, in the order they are declared above.
    @UnannotatedFor("nullness")
    static class Excluded {
        Object get() {
            return null;
        }

        void set(Object of) {}
    }

    void use(Excluded e) {
        // 1: OK, 2: OK, 3: Err -- only conservative defaults make the return @Nullable.
        e.get().toString();
        // 1: Err, 2: Err, 3: Err.  Conservative defaults protect field and method *reads*, not
        // arguments, so this stays an error under all three; it matches the unannotated-code
        // baseline in AnnotatedForWithUse.java, which carries the same TODO.
        e.set(null);
    }
}
