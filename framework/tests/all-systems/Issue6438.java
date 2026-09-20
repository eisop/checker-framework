// Test case for Issue 6438:
// https://github.com/typetools/checker-framework/issues/6438

import java.util.Optional;

abstract class Issue6438 {
    <S extends First & Second> Optional<S> a(boolean b, Class<S> clazz) {
        return b ? Optional.empty() : Optional.of(b(clazz));
    }

    abstract <T extends First & Second> T b(Class<T> clazz);

    interface First {}

    interface Second {}
}
