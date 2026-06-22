import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;

public class AnnotatedSupertype {

    class NullableSupertype
            // :: error: (annotation.on.supertype)
            extends @Nullable Object
            // :: error: (annotation.on.supertype)
            implements @Nullable Serializable {}

    @NonNull class NonNullSupertype
            // :: error: (annotation.on.supertype)
            extends @NonNull Object
            // :: error: (annotation.on.supertype)
            implements @NonNull Serializable {}
}
