import org.checkerframework.checker.nullness.qual.Nullable;

/*
 * @test
 *
 * @summary Test case for printing the type of null literal and wildcard/generics lower bound
 *
 * @compile/fail/ref=NullTypeTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker NullTypeTest.java
 */
public class NullTypeTest {
    Object nn = null;

    @Nullable A<A<Object>> a;

    class A<@Nullable T> {}
}
