package org.springframework.util;

import java.util.Collection;
import java.util.Map;

/** Minimal stand-in for Spring's CollectionUtils, for testing spring.astub. */
public abstract class CollectionUtils {
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }
}
