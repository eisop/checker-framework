// Test case for EISOP Issue 2083:
// https://github.com/eisop/checker-framework/issues/2083

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class EisopIssue2083<T> {
    void primitiveArray(List<? extends int[]> in) {
        new ArrayList<>(in).forEach((int[] b) -> {});
    }

    void referenceArray(List<? extends String[]> in) {
        new ArrayList<>(in).forEach((String[] b) -> {});
    }

    static <Z> void each(List<? extends Z> a, List<? extends Z> b, Consumer<Z> c) {}

    void twoLowerBounds(List<int[]> a, List<? extends int[]> l) {
        each(a, l, b -> {});
    }

    // The lower bound of the capture of ? super T is a type variable without primary annotations.

    static class Token<X> {
        static <X> Token<X> of(Class<X> c) {
            return new Token<>();
        }
    }

    Class<? super T> declaring() {
        throw new Error();
    }

    @SuppressWarnings("unchecked")
    Token<T> superCapture() {
        return (Token<T>) Token.of(declaring());
    }

    // The lower bound of the capture of ? super @Nullable T is a re-qualified type variable.
    Object reQualifiedSuperCapture() {
        return Token.of(reQualified());
    }

    Class<? super @Nullable T> reQualified() {
        throw new Error();
    }

    @SuppressWarnings("lock:methodref.receiver") // Also reported for List<Class<String>>.
    List<Class<? super T>> superCaptureMemberReference(List<Class<? super T>> l) {
        return l.stream().filter(Class::isInterface).collect(Collectors.toList());
    }
}
