import org.checkerframework.checker.fenum.qual.Fenum;

public class CatchFenumUnqualified {
    void method() {
        try {
        } catch (
                // :: error: (exception.parameter.incompatible)
                @Fenum("A") RuntimeException e) {

        }
        try {
            // :: error: (exception.parameter.incompatible)
        } catch (@Fenum("A") NullPointerException | @Fenum("A") ArrayIndexOutOfBoundsException e) {

        }
    }
}
