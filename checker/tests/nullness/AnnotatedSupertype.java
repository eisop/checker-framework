import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.ArrayList;

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

    class NullableParameterizedSupertype
            // :: error: (annotation.on.supertype)
            extends @Nullable ArrayList<String>
            // :: error: (annotation.on.supertype)
            implements @Nullable Comparable<NullableParameterizedSupertype> {
        public int compareTo(NullableParameterizedSupertype other) {
            return 0;
        }
    }

    class QualifiedSupertype
            // :: error: (annotation.on.supertype)
            extends java.util.@Nullable ArrayList<String>
            // :: error: (annotation.on.supertype)
            implements java.io.@Nullable Serializable {}
}
