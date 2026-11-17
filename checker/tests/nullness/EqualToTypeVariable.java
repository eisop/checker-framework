import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Refining an operand of == by the other operand's value, where one of them is a use of a type
// variable without a primary annotation. Such a use stands for its instantiation's qualifier,
// which is only known to be below the type variable's upper bound. Each comparison is written in
// both orders, because the refinement treats its receiver and argument differently. Combining
// with the type variable's lower bound instead made the unannotated operand non-null in
// sameTypeVariable and differentTypeVariables.
public class EqualToTypeVariable<T> {
    @MonotonicNonNull T f;

    void sameTypeVariable(T b) {
        if (f == b) {
            // f may still be null, so b may be null.
            // :: error: (dereference.of.nullable)
            b.toString();
        }
        if (b == f) {
            // :: error: (dereference.of.nullable)
            b.toString();
        }
    }

    void nullableObject(T b, @Nullable Object o) {
        if (o == b) {
            // :: error: (dereference.of.nullable)
            b.toString();
        }
        if (b == o) {
            // :: error: (dereference.of.nullable)
            b.toString();
        }
    }

    <S, U extends @NonNull Object> void differentTypeVariables(S s, @Nullable U u) {
        if (u == s) {
            // s may be null, so u may be null.
            // :: error: (dereference.of.nullable)
            u.toString();
        }
        if (s == u) {
            // :: error: (dereference.of.nullable)
            u.toString();
        }
    }

    // Equality with a non-null value does make the other operand non-null.
    void nonNullTypeVariable(@NonNull T a, T b) {
        if (a == b) {
            b.toString();
        }
        if (b == a) {
            b.toString();
        }
    }

    void nonNullObject(T b, Object o) {
        if (o == b) {
            b.toString();
        }
        if (b == o) {
            b.toString();
        }
    }
}
