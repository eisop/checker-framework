// Test case for EISOP issue #782:
// https://github.com/eisop/checker-framework/issues/782
public class ReceiverTypeNonNullTest {
    public ReceiverTypeNonNullTest() {}

    public ReceiverTypeNonNullTest(Object... objs) {
        super();
    }

    public ReceiverTypeNonNullTest(int i) {
        this();
    }

    public ReceiverTypeNonNullTest(int i, Object... objs) {
        this(objs);
    }
}
