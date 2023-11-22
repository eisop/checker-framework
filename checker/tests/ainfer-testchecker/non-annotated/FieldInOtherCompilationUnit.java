import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

import java.util.GregorianCalendar;

public class FieldInOtherCompilationUnit {

    static @AinferSibling1 int myTime;

    static void test() {
        new GregorianCalendar() {
            public void newMethod() {
                this.time = myTime;
            }
        };
    }
}
