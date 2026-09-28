import org.checkerframework.framework.qual.UnannotatedFor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

// @NullUnmarked aliases to @UnannotatedFor("nullnessnoinit"), the inverse of @NullMarked's
// @AnnotatedFor alias, so it subtracts its scope from an enclosing @NullMarked.
public class NullUnmarkedScope {

    static void take(Object nn) {}

    @NullMarked
    static class Marked {
        void checked(@Nullable Object nble) {
            // :: error: (argument.type.incompatible)
            take(nble);
        }

        @NullUnmarked
        void excluded(@Nullable Object nble) {
            take(nble);
        }

        // A written @UnannotatedFor for another checker must not hide the @NullUnmarked alias:
        // both are collected, so this method is still excluded for nullness.
        @UnannotatedFor("index")
        @NullUnmarked
        void excludedAlongsideWritten(@Nullable Object nble) {
            take(nble);
        }

        @NullUnmarked
        static class ExcludedClass {
            void unchecked(@Nullable Object nble) {
                take(nble);
            }

            // A nested @NullMarked takes effect again inside an excluded element.
            @NullMarked
            void remarked(@Nullable Object nble) {
                // :: error: (argument.type.incompatible)
                take(nble);
            }
        }

        @NullUnmarked
        static class ExcludedConstructor {
            ExcludedConstructor(@Nullable Object nble) {
                take(nble);
            }
        }
    }
}
