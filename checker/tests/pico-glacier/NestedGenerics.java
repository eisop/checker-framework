import java.util.Iterator;

interface Predicate<T> {
    boolean apply(T var1);

    boolean equals(Object var1);
}

final class Iterators {

    public static <T> boolean any(Iterator<T> iterator, Predicate<? super T> predicate) {
        // return indexOf(iterator, predicate) != -1;
        return true;
    }

    public static boolean contains(Iterator<?> iterator, Object element) {
        return any(iterator, null);
    }
}
