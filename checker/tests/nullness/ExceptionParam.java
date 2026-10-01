import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

/** Exception parameters are non-null, even if the default is nullable. */
@DefaultQualifier(org.checkerframework.checker.nullness.qual.Nullable.class)
public class ExceptionParam {
    void exc1() {
        try {
        } catch (AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc2() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@NonNull AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void exc3() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@Nullable AssertionError e) {
            @NonNull Object o = e;
        }
    }

    void multiCatchLeading() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@Nullable AssertionError | RuntimeException e) {
            @NonNull Object o = e;
        }
    }

    void multiCatchTrailing() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (AssertionError | @Nullable RuntimeException e) {
            @NonNull Object o = e;
        }
    }

    // Both alternatives annotated: javac puts the leading @Nullable on the parameter's modifiers
    // and leaves the trailing one on its own alternative, so the two are collected by different
    // paths and neither is lost or double-counted.
    void multiCatchBoth() {
        try {
            // :: warning: (nullness.on.exception.parameter)
        } catch (@Nullable AssertionError | @Nullable RuntimeException e) {
            @NonNull Object o = e;
        }
    }
}
