/*
 * @test
 *
 * @summary Read-write sensitivity applies to a field that comes from unannotated bytecode, under
 * -AuseConservativeDefaultsForUncheckedCode=bytecode.  The treatment of a bytecode field must not
 * depend on whether conservative defaults were also requested for source.
 * See https://github.com/eisop/checker-framework/issues/1358 .
 *
 * @compile UncheckedLib.java
 * @compile/fail/ref=ReadWriteDynamicBytecode.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=bytecode ReadWriteDynamicBytecode.java
 */
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class ReadWriteDynamicBytecode {
    void read(UncheckedLib u) {
        // The field is read, so it is @Nullable.
        @NonNull Object o = u.f;
    }

    void write(UncheckedLib u) {
        // The field is written, so it requires @NonNull.
        u.f = null;
    }
}
