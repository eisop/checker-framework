// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/CollectionUtils.java

package org.springframework.util;

import java.util.Collection;
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
}
