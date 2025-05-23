/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @requires jdk.version.major >= 25
 * @compile ../PersistUtil25.java Driver.java ReferenceInfoUtil.java Implements25.java ../inheritDeclAnnoPersist/AbstractClass.java
 * @run main Driver Implements25
 */

public class Implements25 {

    @ADescriptions({
        @ADescription(annotation = "org/checkerframework/checker/nullness/qual/EnsuresNonNull")
    })
    public String m1() {
        return TestWrapper.wrap(
                "public Test() { f = new Object(); }",
                "@Override public void setf() { f = new Object(); }",
                "@Override public void setg() {}");
    }
}

class TestWrapper {
    public static String wrap(String... method) {
        return String.join(
                System.lineSeparator(),
                "class Test extends AbstractClass {",
                String.join(System.lineSeparator(), method),
                "}");
    }
}
