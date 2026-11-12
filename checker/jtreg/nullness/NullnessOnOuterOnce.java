/*
 * @test
 * @summary Test that a nullness annotation on an outer type is reported exactly once, whether it
 * is a declaration annotation or written within the type.
 *
 * @compile/fail/ref=NullnessOnOuterOnce.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker NullnessOnOuterOnce.java
 */

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

abstract class NullnessOnOuterOnce {
    class Inner {
        class Deep {}
    }

    abstract void declaration(@Nullable NullnessOnOuterOnce.Inner p);

    abstract void withinType(NullnessOnOuterOnce.@Nullable Inner.Deep p);

    abstract void withinTypeAndInner(NullnessOnOuterOnce.@Nullable Inner.@Nullable Deep p);

    abstract void typeArgument(List<NullnessOnOuterOnce.@Nullable Inner.Deep> p);

    abstract NullnessOnOuterOnce.@Nullable Inner.Deep returnType();

    Object cast(Object o) {
        return (NullnessOnOuterOnce.@Nullable Inner.Deep) o;
    }
}
