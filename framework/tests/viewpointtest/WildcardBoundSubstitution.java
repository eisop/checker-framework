// Test case for EISOP Issue 863:
// https://github.com/eisop/checker-framework/issues/863

public class WildcardBoundSubstitution<
        Factory extends WildcardBoundSubstitution.FactoryBase<?, ?>> {

    static class Value<V extends Value<V>> {}

    static class Store<V extends Value<V>, S extends Store<V, S>> {}

    public abstract static class FactoryBase<
            V extends Value<V>,
            S extends Store<V, S>> {
        abstract S getStore();
    }

    void use(Factory factory) {
        Store<?, ?> store = factory.getStore();
    }
}
