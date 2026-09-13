/*
 * @test
 *
 * @summary A field of a class that is not annotated for the Nullness Checker is read-write
 * sensitive under conservative defaults: Nullable where it is read, NonNull where it is written.
 * See https://github.com/eisop/checker-framework/issues/1358 .
 *
 * @compile/fail/ref=ReadWriteDynamicField.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker -AuseConservativeDefaultsForUncheckedCode=source ReadWriteDynamicField.java
 */
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

public class ReadWriteDynamicField {
    class Unannotated {
        Object f;

        Object g;
    }

    @AnnotatedFor("nullness")
    class Use {
        void read(Unannotated u) {
            // The field is read, so it is @Nullable.
            @NonNull Object o = u.f;
        }

        void writeNull(Unannotated u) {
            // The field is written, so it requires @NonNull.
            u.f = null;
        }

        void writeNullable(Unannotated u, @Nullable Object v) {
            u.f = v;
        }

        void writeNonNull(Unannotated u, @NonNull Object v) {
            // OK: the field is written, and a @NonNull value satisfies that.
            u.f = v;
        }

        void fieldToField(Unannotated a, Unannotated b) {
            // Both sides are dynamic, and are resolved independently: the left-hand side is
            // written so it requires @NonNull, and the right-hand side is read so it is @Nullable.
            a.f = b.g;
        }
    }
}
