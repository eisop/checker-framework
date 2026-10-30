// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/ObjectUtils.java

package org.springframework.util;

/** Minimal stand-in for Spring's ObjectUtils, for testing spring.astub. */
public abstract class ObjectUtils {
    /**
     * Returns true if the given array is null or has no elements.
     *
     * @param array the array to check
     * @return true if {@code array} is null or empty
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Returns true if the given object is null. (Spring's version also treats empty arrays,
     * collections, maps, strings, and Optionals as empty.)
     *
     * @param obj the object to check
     * @return true if {@code obj} is null
     */
    public static boolean isEmpty(Object obj) {
        return obj == null;
    }

    /**
     * Returns true if the given object is an array.
     *
     * @param obj the object to check
     * @return true if {@code obj} is non-null and an array
     */
    public static boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }

    /**
     * Returns true if the given array contains the given element.
     *
     * @param array the array to check
     * @param element the element to look for
     * @return true if {@code array} is non-null and contains {@code element}
     */
    public static boolean containsElement(Object[] array, Object element) {
        if (array == null) {
            return false;
        }
        for (Object arrayEle : array) {
            if (arrayEle == null ? element == null : arrayEle.equals(element)) {
                return true;
            }
        }
        return false;
    }
}
