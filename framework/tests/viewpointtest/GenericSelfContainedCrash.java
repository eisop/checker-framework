// Test case for EISOP issue #786:
// https://github.com/eisop/checker-framework/issues/786
public class GenericSelfContainedCrash {
    interface Box<E> {}

    static class BoxImpl<E> implements Box<E> {}

    protected BoxImpl<Integer> field;
}
