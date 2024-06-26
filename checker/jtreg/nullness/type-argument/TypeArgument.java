/*
 * @test
 * @summary Test case for type argument in method invocation.
 *
 * @compile/fail/ref=TypeArgument.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker TypeArgument.java
 */
public class TypeArgument<T> {

    TypeArgument() {
        foo();
    }

    void foo() {}
}
