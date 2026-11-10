import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.AnnotatedFor;

// Regression test for https://github.com/eisop/checker-framework/issues/1358 : under
// -AuseConservativeDefaultsForUncheckedCode, an unannotated field is read-write dynamic. A read
// yields @Nullable, so reading it into a @NonNull local errors, and a write requires @NonNull,
// so writing null into it errors.
public class FieldWrite {
    class Unannotated {
        // No explicit nullness qualifier: conservative defaults apply to this field.
        Object field = new Object();
    }

    @AnnotatedFor("nullness")
    class AnnotatedUse {
        void use(Unannotated u) {
            // ::error: (assignment.type.incompatible)
            @NonNull Object read = u.field;

            // ::error: (assignment.type.incompatible)
            u.field = null;
        }
    }
}
