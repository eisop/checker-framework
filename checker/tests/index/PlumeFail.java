// Test case affected by eisop Issue 22:
// https://github.com/eisop/checker-framework/issues/22

// @skip-test until the issue is fixed

import java.util.Arrays;
import org.checkerframework.common.value.qual.MinLen;

public class PlumeFail {
    void method() {
        @SuppressWarnings({"index", "value"})
        String @MinLen(1) [] args = getArray();
        String[] argArray = Arrays.copyOfRange(args, 1, args.length);
    }

    String[] getArray() {
        return null;
    }
}
