// Test case for EISOP issue #2290:
// https://github.com/eisop/checker-framework/issues/2290
// javac gives a type annotation on an array creation whose element type has a wildcard type
// argument an UNKNOWN position. ParamApplier reads the enclosing method's type annotations and
// used to reject that position as invalid.

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;

@SuppressWarnings("all") // Only check for crashes.
public class Issue2290 {
    static void withParam(Object p) {
        Object x = new @NonNull List<?>[0];
    }

    static void withoutParam() {
        Object x = new @NonNull List<?>[0];
    }

    static void noWildcard(Object p) {
        Object x = new @NonNull Object[0];
    }

    static void unannotated(Object p) {
        Object x = new List<?>[0];
    }

    // Only unbounded wildcards are legal in a generic array creation.
    static void moreWildcards(Object p, Object q) {
        Object x = new @NonNull Map<?, ?>[0];
        Object y = new List<@NonNull ?>[0];
        Object z = new @NonNull List<?>[0][0];
    }

    static void nestedArray(Object p) {
        Object x = new @NonNull List<?>[0][];
        Object y = new List<?> @NonNull [0][];
        Object z = new List<?>[0] @NonNull [];
    }

    static void lambdaParam(Object p) {
        Runnable r =
                () -> {
                    Object x = new @NonNull List<?>[0];
                };
        java.util.function.Consumer<Object> c =
                o -> {
                    Object x = new @NonNull List<?>[0];
                };
    }

    Issue2290(Object p) {
        Object x = new @NonNull List<?>[0];
    }

    static <T> void generic(T p) {
        Object x = new @NonNull List<?>[0];
    }
}
