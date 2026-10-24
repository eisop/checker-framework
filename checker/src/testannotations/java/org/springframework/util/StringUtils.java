// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/StringUtils.java

package org.springframework.util;

/** Minimal stand-in for Spring's StringUtils, for testing spring.astub. */
public abstract class StringUtils {
    /**
     * Returns true if the given object is null or the empty string.
     *
     * @param str the object to check
     * @return true if {@code str} is null or the empty string
     */
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    /**
     * Returns true if the given character sequence is non-null and has length greater than 0.
     *
     * @param str the character sequence to check
     * @return true if {@code str} is non-null and non-empty
     */
    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }

    /**
     * Returns true if the given string is non-null and has length greater than 0.
     *
     * @param str the string to check
     * @return true if {@code str} is non-null and non-empty
     */
    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Returns true if the given character sequence is non-null and non-empty. (Spring's version
     * also requires a non-whitespace character.)
     *
     * @param str the character sequence to check
     * @return true if {@code str} is non-null and non-empty
     */
    public static boolean hasText(CharSequence str) {
        return str != null && str.length() > 0;
    }

    /**
     * Returns true if the given string is non-null and non-empty. (Spring's version also requires a
     * non-whitespace character.)
     *
     * @param str the string to check
     * @return true if {@code str} is non-null and non-empty
     */
    public static boolean hasText(String str) {
        return str != null && !str.isEmpty();
    }
}
