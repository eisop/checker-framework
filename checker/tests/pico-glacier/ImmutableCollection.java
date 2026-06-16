import org.checkerframework.checker.pico.qual.Immutable;

import java.util.*;

@Immutable public abstract class ImmutableCollection<E> extends AbstractCollection<E> {
    public int getSize() {
        return size();
    }
}
