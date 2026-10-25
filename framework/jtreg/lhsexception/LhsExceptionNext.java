import org.checkerframework.framework.testchecker.util.Odd;

public class LhsExceptionNext {
    void refine(@Odd String odd) {
        String s = odd;
        // Type-checks only if s is refined to @Odd.
        @Odd String t = s;
    }
}
