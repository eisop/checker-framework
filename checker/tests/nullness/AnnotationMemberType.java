import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An annotation element's value must be a constant expression (JLS 9.7.1); {@code null} is never a
 * constant expression, for any element type, so a nullness annotation on any component of an
 * annotation interface member's return type is always either a contradiction or redundant.
 */
public class AnnotationMemberType {
    @interface Unannotated {
        String value();
    }

    @interface RootNonNull {
        // :: error: (nullness.on.annotation.member)
        @NonNull String value();
    }

    @interface RootNullable {
        // :: error: (nullness.on.annotation.member)
        @Nullable String value();
    }

    @interface ComponentNullable {
        // "Any component", so an array's element type is reported too.
        // :: error: (nullness.on.annotation.member)
        @Nullable String[] value();
    }
}
