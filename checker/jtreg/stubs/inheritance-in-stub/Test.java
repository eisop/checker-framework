/*
 * @test
 * @summary Test default and inheritance.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Pet.java
 * @compile -XDrawDiagnostics -Xlint:unchecked Cat.java
 * @compile -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=cat.astub -Werror Test.java
 */

public class Test {
    void foo() {
        Cat c = new Cat();
        c.playWith(null);
    }
}
