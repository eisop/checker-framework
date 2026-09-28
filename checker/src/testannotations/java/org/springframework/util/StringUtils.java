package org.springframework.util;

/** Minimal stand-in for Spring's StringUtils, for testing spring.astub. */
public abstract class StringUtils {
    public static boolean isEmpty(Object str) {
        return str == null || "".equals(str);
    }

    public static boolean hasLength(CharSequence str) {
        return str != null && str.length() > 0;
    }

    public static boolean hasLength(String str) {
        return str != null && !str.isEmpty();
    }

    public static boolean hasText(CharSequence str) {
        return str != null && str.length() > 0;
    }

    public static boolean hasText(String str) {
        return str != null && !str.isEmpty();
    }
}
