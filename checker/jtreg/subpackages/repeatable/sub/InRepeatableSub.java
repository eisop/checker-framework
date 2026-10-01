package repeatable.sub;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package repeatable's two @AnnotatedFor entries apply independently: nullness set
 * applyToSubpackages=false, so this subpackage is out of its scope; index set
 * applyToSubpackages=true, so this subpackage is still in its scope. Each @compile line below
 * checks this with one checker at a time, since running both checkers in the same javac invocation
 * is not how this is exercised here -- see the class comment on the driving test.
 */
public class InRepeatableSub {
    void takeNonNull(Object nn) {}

    void nullness(@Nullable Object n) {
        takeNonNull(n);
    }

    void takeNonNegative(@NonNegative int i) {}

    void index(int i) {
        takeNonNegative(i);
    }
}
