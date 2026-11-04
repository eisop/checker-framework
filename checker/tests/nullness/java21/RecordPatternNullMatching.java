// @below-java21-jdk-skip-test

// A type pattern nested in a record pattern that is unconditional for the record component's type
// matches null (JLS 14.30.1), so its binding has the component's type, including the component's
// nullness.  A nested type pattern that is not unconditional, and a top-level type pattern, do
// not match null.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RecordPatternNullMatching {

    record P(String a, @Nullable String b) {}

    record Q(@Nullable Object c) {}

    record R(@Nullable P p) {}

    record G<T extends @Nullable Object>(T t) {}

    int explicitType(Object o) {
        if (o instanceof P(String a, String b)) {
            a.length();
            // :: error: (dereference.of.nullable)
            return b.length();
        }
        if (o instanceof Q(Object c)) {
            // :: error: (dereference.of.nullable)
            return c.hashCode();
        }
        return 0;
    }

    int varPattern(Object o) {
        if (o instanceof P(var a, var b)) {
            a.length();
            // :: error: (dereference.of.nullable)
            return b.length();
        }
        if (o instanceof Q(var c)) {
            // :: error: (dereference.of.nullable)
            return c.hashCode();
        }
        return 0;
    }

    int nestedRecordPattern(Object o) {
        // A nested record pattern does not match null, so its components are matched only
        // against a non-null record.
        if (o instanceof R(P(String a, String b))) {
            a.length();
            // :: error: (dereference.of.nullable)
            return b.length();
        }
        if (o instanceof R(P p)) {
            // :: error: (dereference.of.nullable)
            return p.hashCode();
        }
        return 0;
    }

    int genericRecord(Object o) {
        // The component's type is a type variable whose bound is @Nullable.
        if (o instanceof G<?>(Object t)) {
            // :: error: (dereference.of.nullable)
            return t.hashCode();
        }
        return 0;
    }

    int refinedAfterMatch(Object o) {
        if (o instanceof P(String a, String b) && b != null) {
            return b.length();
        }
        return 0;
    }

    int notUnconditional(Object o) {
        // CharSequence is not unconditional for Object, so null does not match.
        if (o instanceof Q(CharSequence c)) {
            return c.length();
        }
        // :: error: (instanceof.nullable)
        if (o instanceof Q(@Nullable CharSequence c)) {}
        // :: warning: (instanceof.nonnull.redundant)
        if (o instanceof Q(@NonNull CharSequence c)) {}
        return 0;
    }

    int nullableAnnotationAccepted(@Nullable Object o) {
        if (o instanceof P(String a, @Nullable String b)) {
            // :: error: (dereference.of.nullable)
            return b.length();
        }
        if (o instanceof Q(@Nullable Object c)) {
            // :: error: (dereference.of.nullable)
            return c.hashCode();
        }
        return 0;
    }

    void nonNullOnNullableComponent(Object o) {
        // :: error: (assignment.type.incompatible)
        if (o instanceof P(String a, @NonNull String b)) {}
    }

    int nonNullComponent(Object o) {
        if (o instanceof P(String a, String b)) {
            return a.length();
        }
        if (o instanceof P(@NonNull String a, String b)) {
            return a.length();
        }
        return 0;
    }

    int switchPattern(Object o) {
        return switch (o) {
            case P(String a, String b) ->
                    // :: error: (dereference.of.nullable)
                    b.length();
            case Q(CharSequence c) -> c.length();
            default -> 0;
        };
    }

    int topLevel(@Nullable Object o) {
        if (o instanceof String s) {
            return s.length();
        }
        // :: error: (instanceof.nullable)
        if (o instanceof @Nullable String s) {}
        return 0;
    }
}
