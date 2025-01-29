package org.checkerframework.checker.testchecker.ainfer.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1
 * @see AinferSibling2
 * @see AinferParent
 */
@SubtypeOf({})
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface AinferTop {}
