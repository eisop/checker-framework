package org.checkerframework.framework.source;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The values of the {@code -Amode} option that a checker supports. Each names a group of options
 * that the checker turns on, defined by its {@link SourceChecker#addOptionsForMode} override.
 *
 * <p>{@link SourceChecker#getSupportedModes} collects these annotations from the checker's class
 * hierarchy and from its subcheckers; write this annotation on the class whose {@code
 * addOptionsForMode} handles the mode.
 *
 * @see SupportedOptions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface SupportedModes {
    /**
     * Returns the supported mode names.
     *
     * @return the supported mode names
     */
    String[] value();
}
