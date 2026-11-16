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

    /**
     * Returns true if the given character sequence contains a whitespace character.
     *
     * @param str the character sequence to check
     * @return true if {@code str} is non-null and contains a whitespace character
     */
    public static boolean containsWhitespace(CharSequence str) {
        if (str == null) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given string contains a whitespace character.
     *
     * @param str the string to check
     * @return true if {@code str} is non-null and contains a whitespace character
     */
    public static boolean containsWhitespace(String str) {
        return containsWhitespace((CharSequence) str);
    }

    /**
     * Returns true if the given string consists of exactly the given character.
     *
     * @param str the string to check
     * @param singleCharacter the character to compare to
     * @return true if {@code str} is non-null and equal to {@code singleCharacter}
     */
    public static boolean matchesCharacter(String str, char singleCharacter) {
        return str != null && str.length() == 1 && str.charAt(0) == singleCharacter;
    }

    /**
     * Returns true if the given string starts with the given prefix, ignoring case.
     *
     * @param str the string to check
     * @param prefix the prefix to look for
     * @return true if {@code str} and {@code prefix} are non-null and {@code str} starts with
     *     {@code prefix}, ignoring case
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str != null
                && prefix != null
                && str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Returns true if the given string ends with the given suffix, ignoring case.
     *
     * @param str the string to check
     * @param suffix the suffix to look for
     * @return true if {@code str} and {@code suffix} are non-null and {@code str} ends with {@code
     *     suffix}, ignoring case
     */
    public static boolean endsWithIgnoreCase(String str, String suffix) {
        return str != null
                && suffix != null
                && str.length() >= suffix.length()
                && str.regionMatches(
                        true, str.length() - suffix.length(), suffix, 0, suffix.length());
    }
}
