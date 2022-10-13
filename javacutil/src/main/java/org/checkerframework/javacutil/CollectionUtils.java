package org.checkerframework.javacutil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Utility methods related to Java Collections. */
public class CollectionUtils {

    /** The class object of "java.util.Collections$UnmodifiableCollection" */
    private static final Class<?> unmodifiableCollectionClass;

    static {
        try {
            unmodifiableCollectionClass =
                    Class.forName("java.util.Collections$UnmodifiableCollection");
        } catch (ClassNotFoundException e) {
            throw new BugInCF(e);
        }
    }

    /**
     * Creates a LRU cache.
     *
     * @param size size of the cache
     * @return a new cache with the provided size
     */
    public static <K, V> Map<K, V> createLRUCache(final int size) {
        return new LinkedHashMap<K, V>(size, .75F, true) {

            private static final long serialVersionUID = 5261489276168775084L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
                return size() > size;
            }
        };
    }

    /**
     * Determines if the passed argument is an instance of
     * "java.util.Collections$UnmodifiableCollection".
     *
     * @param c the collection to check
     * @return true if {@code c} is an instance of "java.util.Collections$UnmodifiableCollection"
     */
    public static boolean isUnmodifiableCollection(Collection<?> c) {
        return unmodifiableCollectionClass.isInstance(c);
    }
}
