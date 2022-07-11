/*
 * @test
 * @summary Defaults applied on class will not be inherited.
 *
 * @compile -XDrawDiagnostics -Xlint:unchecked Pet.java
 * @compile -XDrawDiagnostics -Xlint:unchecked Cat.java
 * @compile/fail/ref=PetTest.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Anomsgtext -Astubs=pet.astub -Werror PetTest.java
 */

public class PetTest {
    void foo() {
        Cat c = new Cat();
        c.playWith(null);
        Pet p = c;
        // Currently, we trust the annotations in astub files.
        // TODO: issue a warning for this unsound behavior
        p.playWith(null);
    }
}
