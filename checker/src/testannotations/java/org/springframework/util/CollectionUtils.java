// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/CollectionUtils.java

package org.springframework.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/** Minimal stand-in for Spring's CollectionUtils, for testing spring.astub. */
public abstract class CollectionUtils {
    /**
     * Returns true if the given collection is null or empty.
     *
     * @param collection the collection to check
     * @return true if {@code collection} is null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Returns true if the given map is null or empty.
     *
     * @param map the map to check
     * @return true if {@code map} is null or empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Returns true if the given iterator contains the given element.
     *
     * @param iterator the iterator to check
     * @param element the element to look for
     * @return true if {@code iterator} is non-null and contains {@code element}
     */
    public static boolean contains(Iterator<?> iterator, Object element) {
        if (iterator != null) {
            while (iterator.hasNext()) {
                Object candidate = iterator.next();
                if (candidate == null ? element == null : candidate.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given enumeration contains the given element.
     *
     * @param enumeration the enumeration to check
     * @param element the element to look for
     * @return true if {@code enumeration} is non-null and contains {@code element}
     */
    public static boolean contains(Enumeration<?> enumeration, Object element) {
        if (enumeration != null) {
            while (enumeration.hasMoreElements()) {
                Object candidate = enumeration.nextElement();
                if (candidate == null ? element == null : candidate.equals(element)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the given collection contains the given element instance.
     *
     * @param collection the collection to check
     * @param element the element to look for
     * @return true if {@code collection} is non-null and contains {@code element} itself
     */
    public static boolean containsInstance(Collection<?> collection, Object element) {
        if (collection != null) {
            for (Object candidate : collection) {
                if (candidate == element) {
                    return true;
                }
            }
        }
        return false;
    }
}
