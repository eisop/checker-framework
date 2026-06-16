public class GenericSelfContainedCrash {
    interface Box<E> {}

    static class BoxImpl<E> implements Box<E> {}

    protected BoxImpl<Integer> field;
}
