package af.sub.deep;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Package af.sub sets applyToSubpackages=false, which limits its own annotation to af.sub. It does
 * not block package af, whose annotation applies to subpackages and so still reaches here. This
 * code is therefore inside an AnnotatedFor scope and its warnings are issued; if the walk up the
 * package chain stopped at af.sub, conservative defaults would suppress them.
 */
public class Deep {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
