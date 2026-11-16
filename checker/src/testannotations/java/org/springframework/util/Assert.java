// Upstream version (this is a clean-room reimplementation of its interface):
// https://github.com/spring-projects/spring-framework/blob/main/spring-core/src/main/java/org/springframework/util/Assert.java

package org.springframework.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/** Minimal stand-in for Spring's Assert, for testing spring.astub. */
public abstract class Assert {
    /**
     * Throws IllegalArgumentException if {@code object} is null.
     *
     * @param object the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code object} is null
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code object} is null.
     *
     * @param object the object to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code object} is null
     */
    public static void notNull(Object object, Supplier<String> messageSupplier) {
        if (object == null) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code text} is null or empty.
     *
     * @param text the string to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public static void hasLength(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code text} is null or empty.
     *
     * @param text the string to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public static void hasLength(String text, Supplier<String> messageSupplier) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code text} is null or empty (Spring's version also
     * rejects a string that contains only whitespace).
     *
     * @param text the string to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public static void hasText(String text, String message) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code text} is null or empty (Spring's version also
     * rejects a string that contains only whitespace).
     *
     * @param text the string to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code text} is null or empty
     */
    public static void hasText(String text, Supplier<String> messageSupplier) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code array} is null or has no elements.
     *
     * @param array the array to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code array} is null or has no elements
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code array} is null or has no elements.
     *
     * @param array the array to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code array} is null or has no elements
     */
    public static void notEmpty(Object[] array, Supplier<String> messageSupplier) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code collection} is null or empty.
     *
     * @param collection the collection to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code collection} is null or empty
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code collection} is null or empty.
     *
     * @param collection the collection to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code collection} is null or empty
     */
    public static void notEmpty(Collection<?> collection, Supplier<String> messageSupplier) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code map} is null or empty.
     *
     * @param map the map to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code map} is null or empty
     */
    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code map} is null or empty.
     *
     * @param map the map to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code map} is null or empty
     */
    public static void notEmpty(Map<?, ?> map, Supplier<String> messageSupplier) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalStateException if {@code expression} is false.
     *
     * @param expression a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Throws IllegalStateException if {@code expression} is false.
     *
     * @param expression a boolean expression
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void state(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code expression} is false.
     *
     * @param expression a boolean expression
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code expression} is false.
     *
     * @param expression a boolean expression
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
        if (!expression) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code obj} is not an instance of {@code type}.
     *
     * @param type the type to check against
     * @param obj the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code obj} is not an instance of {@code type}
     */
    public static void isInstanceOf(Class<?> type, Object obj, String message) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code obj} is not an instance of {@code type}.
     *
     * @param type the type to check against
     * @param obj the object to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code obj} is not an instance of {@code type}
     */
    public static void isInstanceOf(Class<?> type, Object obj, Supplier<String> messageSupplier) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code obj} is not an instance of {@code type}.
     *
     * @param type the type to check against
     * @param obj the object to check
     * @throws IllegalArgumentException if {@code obj} is not an instance of {@code type}
     */
    public static void isInstanceOf(Class<?> type, Object obj) {
        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException("");
        }
    }

    /**
     * Throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     * superType}.
     *
     * @param superType the supertype to check against
     * @param subType the subtype to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     *     superType}
     */
    public static void isAssignable(Class<?> superType, Class<?> subType, String message) {
        if (subType == null || !superType.isAssignableFrom(subType)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     * superType}.
     *
     * @param superType the supertype to check against
     * @param subType the subtype to check
     * @param messageSupplier a supplier for the exception message to use if the assertion fails
     * @throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     *     superType}
     */
    public static void isAssignable(
            Class<?> superType, Class<?> subType, Supplier<String> messageSupplier) {
        if (subType == null || !superType.isAssignableFrom(subType)) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     * superType}.
     *
     * @param superType the supertype to check against
     * @param subType the subtype to check
     * @throws IllegalArgumentException if {@code subType} is null or not assignable to {@code
     *     superType}
     */
    public static void isAssignable(Class<?> superType, Class<?> subType) {
        if (subType == null || !superType.isAssignableFrom(subType)) {
            throw new IllegalArgumentException("");
        }
    }
}
