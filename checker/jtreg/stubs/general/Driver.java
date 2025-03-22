/*
 * @test
 * @summary Test that java.lang annotations can be used.
 * @library .
 * @compile -processor org.checkerframework.checker.nullness.NullnessChecker -Astubs=MyStub.astub Driver.java -Werror
 */

public class Driver {
    void test() {
        Object o = null;
        String v = String.valueOf(o);
    }
}
