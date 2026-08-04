import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

// Regression test for https://github.com/eisop/checker-framework/issues/1358 :
// -AuseConservativeDefaultsForUncheckedCode is sound for field reads (an unannotated field is
// defaulted to the top qualifier, @Nullable, so reading it into a @NonNull local correctly
// errors) but unsound for field writes (the same read-oriented default is reused as the
// required type of the assignment, so writing null into the field is currently, incorrectly,
// accepted). See QualifierDefaults.STANDARD_UNCHECKED_DEFAULTS_TOP for the root cause.
public class FieldWriteUnsound {
    class Unannotated {
        // No explicit nullness qualifier: conservative defaults apply to this field.
        Object field = new Object();
    }

    @AnnotatedFor("nullness")
    class AnnotatedUse {
        void use(Unannotated u) {
            // Sound: field reads are conservative, so this correctly errors.
            // ::error: (assignment.type.incompatible)
            @NonNull Object read = u.field;

            // TODO(#1358): unsound. Field writes under conservative defaults should also
            // require @NonNull, but this is currently accepted (no error) because
            // QualifierDefaults does not distinguish field-read defaults from field-write
            // defaults. Once fixed, this line should be annotated with:
            //     // ::error: (assignment.type.incompatible)
            // No error is expected below until then; the assignment below documents the
            // currently-unsound behavior, it is not a desired outcome.
            u.field = null;
        }
    }
}
