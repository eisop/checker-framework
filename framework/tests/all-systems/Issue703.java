// Test case for Issue 703:
// https://github.com/eisop/checker-framework/issues/703

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

abstract class Issue703 {
    void foo() {
        List<Issue703> attrs = or(x(), y());
    }

    abstract <T> @NonNull T or(T nullableValue, T defaultValue);

    abstract List<Issue703> x();

    abstract <S> List<S> y();
}
