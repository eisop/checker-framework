/*
 * @test
 * @summary Test that an out-of-bound type argument of an enclosing type in a chain of three or
 * more levels is reported exactly once. See https://github.com/eisop/checker-framework/issues/1926
 *
 * @compile/fail/ref=EnclosingTypeArgsChain.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker EnclosingTypeArgsChain.java
 */

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class EnclosingTypeArgsChain {
    static class A<XXX extends @NonNull Object> {
        class B<YYY extends @NonNull Object> {
            class C {}

            class D<ZZZ extends @NonNull Object> {
                class E {}
            }
        }
    }

    void outer(A<@Nullable String>.B<@NonNull String>.C p) {}

    void middle(A<@NonNull String>.B<@Nullable String>.C p) {}

    void both(A<@Nullable String>.B<@Nullable String>.C p) {}

    void ok(A<@NonNull String>.B<@NonNull String>.C p) {}

    void fourLevels(A<@Nullable String>.B<@NonNull String>.D<@NonNull String>.E p) {}

    abstract A<@Nullable String>.B<@NonNull String>.C returnType();
}
