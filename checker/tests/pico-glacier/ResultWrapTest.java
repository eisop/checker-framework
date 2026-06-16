// import edu.cmu.cs.glacier.qual.MaybeMutable;
import org.checkerframework.checker.pico.qual.Mutable;

public class ResultWrapTest {

    ResultWrapTest() {
        // while visiting this, the return type must be annotated correctly?
    }

    static class ResultWrap<T extends @Mutable Object> {}

    final ResultWrap<String> input = null;
}
