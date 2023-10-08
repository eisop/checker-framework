import org.checkerframework.checker.nullness.qual.*;

import java.io.Serializable;

// :: warning: (explicit.annotation.ignored)
public class Issue3349<T extends @NonNull Object & @Nullable Serializable> {
    void foo(T p1) {
        @NonNull Serializable s = p1;
        @NonNull Object o = p1;
    }
}
