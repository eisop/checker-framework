import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

public class JSpecifyScope {

    static void take(Object nn) {}

    // @NullMarked is aliased to @AnnotatedFor("nullness"), so this class is checked.
    @NullMarked
    static class Marked {
        void m(@Nullable Object nble) {
            // :: error: (argument.type.incompatible)
            take(nble);
        }

        // @NullUnmarked is aliased to @UnannotatedFor("nullness"), which excludes this method
        // from the enclosing @NullMarked scope.
        @NullUnmarked
        void excluded(@Nullable Object nble) {
            take(nble);
        }

        // A nested @NullMarked takes effect again inside an excluded element.
        @NullUnmarked
        static class Excluded {
            void unchecked(@Nullable Object nble) {
                take(nble);
            }

            @NullMarked
            void remarked(@Nullable Object nble) {
                // :: error: (argument.type.incompatible)
                take(nble);
            }
        }

        // A @NullUnmarked constructor is excluded like any other element.
        @NullUnmarked
        static class ExcludedConstructor {
            ExcludedConstructor(@Nullable Object nble) {
                take(nble);
            }
        }
    }

    // Not marked at all, so its warnings are suppressed.
    static class Unmarked {
        void m(@Nullable Object nble) {
            take(nble);
        }
    }

    // A @NullUnmarked element's *signature*, seen from checked code. The parameter has no written
    // annotation, so what the caller sees is whatever default applies to unmarked code -- the
    // effect of @UnannotatedFor that is invisible from inside the excluded scope itself.
    @NullMarked
    static class Uses {
        @NullUnmarked
        static void unmarkedSignature(Object o) {}

        @NullUnmarked
        static <T> void unmarkedTypeVariable(T t) {}

        static void caller(@Nullable Object nble, @Nullable String nbleStr) {
            // -Amode=jspecify implies -AonlyAnnotatedFor, which suppresses warnings in unmarked
            // code without changing its defaults, so the parameter keeps the CLIMB default
            // @NonNull and this argument is rejected.
            // :: error: (argument.type.incompatible)
            unmarkedSignature(nble);
            // @NullUnmarked also undoes the enclosing @NullMarked's upper-bound default, so T is
            // not bounded by @NonNull and this call is accepted.
            unmarkedTypeVariable(nbleStr);
        }
    }
}
