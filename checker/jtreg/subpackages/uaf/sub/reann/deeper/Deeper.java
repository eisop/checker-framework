package uaf.sub.reann.deeper;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The innermost package annotation wins: package uaf.sub.reann is AnnotatedFor and applies to
 * subpackages, so it takes effect again here despite the UnannotatedFor on package uaf.sub. This
 * code is inside an AnnotatedFor scope and its warnings are issued.
 */
public class Deeper {
    void take(Object nn) {}

    void m(@Nullable Object nble) {
        take(nble);
    }
}
