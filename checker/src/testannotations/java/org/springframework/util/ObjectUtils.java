package org.springframework.util;

/** Minimal stand-in for Spring's ObjectUtils, for testing spring.astub. */
public abstract class ObjectUtils {
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(Object obj) {
        return obj == null;
    }
}
