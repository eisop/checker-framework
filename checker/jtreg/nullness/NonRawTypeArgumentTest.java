/*
 * @test
 * @summary Test case for type argument in method invocation.
 *
 * @compile/fail/ref=NonRawTypeArgumentTest.out -XDrawDiagnostics -processor org.checkerframework.checker.nullness.NullnessChecker NonRawTypeArgumentTest.java
 */
public class NonRawTypeArgumentTest<T> {

    NonRawTypeArgumentTest() {
        foo();
    }

    void foo() {}
}
