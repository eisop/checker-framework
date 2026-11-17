// Two wildcard type arguments of an invariant type are equal only if their bounds are equal
// including all qualifiers: the super bound, the extends bound, and the type arguments of either.

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

class EisopIssue2582 {
    static class Box<T> {
        T t;

        Box(T t) {
            this.t = t;
        }
    }

    void superBound(Box<List<? super @Nullable String>> b) {
        // :: error: (assignment.type.incompatible)
        Box<List<? super String>> c = b;
    }

    void superBoundReverse(Box<List<? super String>> b) {
        // :: error: (assignment.type.incompatible)
        Box<List<? super @Nullable String>> c = b;
        // Would add null to a List<String>:
        c.t.add(null);
    }

    void extendsBoundTypeArgument(Box<List<? extends List<@Nullable String>>> b) {
        // :: error: (assignment.type.incompatible)
        Box<List<? extends List<String>>> c = b;
    }

    void extendsBoundTypeArgumentReverse(Box<List<? extends List<String>>> b) {
        // :: error: (assignment.type.incompatible)
        Box<List<? extends List<@Nullable String>>> c = b;
    }

    void extendsBoundQualifier(Box<List<? extends @Nullable Object>> b) {
        // :: error: (assignment.type.incompatible)
        Box<List<? extends Object>> c = b;
    }

    void equalBounds(
            Box<List<? super @Nullable String>> b1,
            Box<List<? super String>> b2,
            Box<List<? extends List<@Nullable String>>> b3,
            Box<List<? extends List<String>>> b4) {
        Box<List<? super @Nullable String>> c1 = b1;
        Box<List<? super String>> c2 = b2;
        Box<List<? extends List<@Nullable String>>> c3 = b3;
        Box<List<? extends List<String>>> c4 = b4;
    }

    static class Bounded<T extends List<@Nullable String>> {}

    // An unbounded wildcard and a wildcard whose extends bound is the declared bound of the type
    // parameter denote the same type.
    void unboundedWildcard(Box<Bounded<?>> b) {
        Box<Bounded<? extends List<@Nullable String>>> c = b;
        Box<Bounded<?>> d = c;
    }
}
