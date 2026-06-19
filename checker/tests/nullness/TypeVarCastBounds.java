import org.checkerframework.checker.nullness.qual.Nullable;

class TypeVarCastBounds<E extends @Nullable Object> {
    private @Nullable Object[] elements = new Object[0];

    @SuppressWarnings("unchecked")
    E get(int index) {
        @Nullable Object result = elements[index];
        return (E) result;
    }
}
