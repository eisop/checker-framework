package permissivedefaultslib;

import java.util.List;

/** Unchecked code: not {@code @AnnotatedFor("nullness")}. */
public class GenericLib<T> {
    public void consume(T value) {}

    public T produce() {
        return null;
    }

    public void consumeAll(List<T> values) {}

    public void unannotated(String value) {}
}
