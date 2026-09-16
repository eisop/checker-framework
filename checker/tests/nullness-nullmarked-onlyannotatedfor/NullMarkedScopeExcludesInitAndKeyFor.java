import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.framework.qual.AnnotatedFor;
import org.jspecify.annotations.NullMarked;

import java.util.Map;

// @NullMarked aliases to @AnnotatedFor("nullnessnoinit"), naming only the nullness-only
// subchecker -- not @AnnotatedFor("nullness"), which would also reach the Initialization and
// KeyFor subcheckers. JSpecify itself defines no initialization or keyfor checking, and
// -Amode=jspecify already excludes both (-AassumeInitialized -AassumeKeyFor), so @NullMarked
// alone should not silently enable them either. A written @AnnotatedFor naming those checkers
// still composes normally: only the alias's own implied scope is narrower.
public class NullMarkedScopeExcludesInitAndKeyFor {

    @NullMarked
    static class OnlyNullMarked {
        Object f;

        // No expected error: @NullMarked does not imply @AnnotatedFor("initialization").
        OnlyNullMarked() {}
    }

    @AnnotatedFor("initialization")
    static class ExplicitInitialization {
        Object f;

        // :: error: (initialization.fields.uninitialized)
        ExplicitInitialization() {}
    }

    @AnnotatedFor("initialization")
    @NullMarked
    static class ExplicitInitializationAndNullMarked {
        Object f;

        // :: error: (initialization.fields.uninitialized)
        ExplicitInitializationAndNullMarked() {}
    }

    @NullMarked
    static void keyForNotChecked(Map<String, Object> map, String s) {
        // No expected error: @NullMarked does not imply @AnnotatedFor("keyfor").
        @KeyFor("map") String k = s;
    }

    @AnnotatedFor("keyfor")
    @NullMarked
    static void keyForChecked(Map<String, Object> map, String s) {
        // :: error: (assignment.type.incompatible)
        @KeyFor("map") String k = s;
    }
}
